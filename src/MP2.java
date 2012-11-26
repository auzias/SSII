/* Fichier : http://cours.polytech.unice.fr/ssii/s12.MiniProjet2009-10/java/src/MP2.java
 * lire un fichier wave, dans un tableau de type byte[],
 * trouver les valeurs des echantillons, en tenant compte du format
 * les sauver dans un fichier texte pour les afficher
 * jouer le signal audio sur les hauts parleurs 
 * Cours Signal Son et Image pour l'Informaticien (S.S.I.I.), Polytech'Nice-Sophia
 * Jean-Paul Stromboni, Joel Leroux, pour le Miniprojet 2009-2010, decembre 2009
 */
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import javax.sound.sampled.*;
import javax.sound.sampled.spi.FormatConversionProvider;

public class MP2 {
	private AudioFormat format;
	private byte[] samples;
	private AudioInputStream stream;
	private long fe;

	public MP2(String filename) {
		try {
			stream = AudioSystem.getAudioInputStream(new File(filename));
			format = stream.getFormat();
			samples = getSamples(stream);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] getSamples() {
		return samples;
	}

	public byte[] getSamples(AudioInputStream stream) {
		int length = (int) (stream.getFrameLength() * format.getFrameSize());
		byte[] samples = new byte[length];
		DataInputStream in = new DataInputStream(stream);
		try {
			in.readFully(samples);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return samples;
	}

	public void play(InputStream source) {
		// definition d'un buffer de duree 100ms= 1/10s (soit fe*(B/8)/10)
		int bufferSize = format.getFrameSize()*Math.round(format.getSampleRate()/10);
		byte[] buffer = new byte[bufferSize];
		SourceDataLine line;
		try {
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format, bufferSize);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			return;
		}
		line.start();
		try {
			int numBytesRead = 0;
			while (numBytesRead != -1) {
				numBytesRead = source.read(buffer, 0, buffer.length);
				if (numBytesRead != -1)
					line.write(buffer, 0, numBytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		line.drain();
		line.close();
	}

	public static void main(String[] args){	
		MP2 player = new MP2("NoteGuitare.wav"); 
		// ou "simple.wav" qui contient simplesin(t)=exp(-3*t)*sin(2*pi*440*t)
		System.out.println(player.format.toString()); //affiche le format du fichier wave
		float fe =player.format.getSampleRate();
		byte[] samples = player.getSamples();
		System.out.println("Nombres d'echantillons: "+samples.length);
		// on fixe a N= 2^17= 131072 la taille de la fenetre de calcul de la FFT
		int N = 131072;
		Complex[] x = new Complex[N];
		for (int i= 0; i<N ; i++) {
			x[i] = new Complex(0,0);
		}
		// calcul de 2^B
		int twoPowB= (int) Math.pow(2,player.format.getSampleSizeInBits());
		
		// on extrait les echantillons en respectant le format bigEndian ou lowEndian
		try {
			FileWriter fw= new FileWriter("result.txt");
			double value;
			int j=0;
			for (int i = 0; i<samples.length ; i+=2) {
				if (!player.format.isBigEndian()) 
					value = (short)((samples[i]& 0xFF)+(samples[i+1]*256));
				else value	 =(short)((samples[i+1] & 0xFF)+(samples[i]*256)); 
				// store the sampled values in result.txt in order to plot
				fw.write(j+" "+String.valueOf(2*value/twoPowB)+"\n");
				x[j++] = new Complex(((double) 2*value)/twoPowB, 0.0);
			}
			System.out.println("# taille fenetre analyse x: "+x.length);
			fw.close();

			// on calcule le spectre du signal x tire de samples
			Complex[] spectre = FFT.fft(x);
			System.out.println("@"+spectre.length);
			FileWriter fw2 = new FileWriter("spectre.txt");
			for(int i=0 ; i<spectre.length ; i++) {
				fw2.write((i*fe/spectre.length)+"\t"+spectre[i].re()+"\t"+spectre[i].im()+"\n");
			}
			fw2.close();

			// on filtre le signal x en modifiant son spectre : 
			// on conserve inchangee la bande de frequence [imin*fe/N,imax*fe/N]
			// on annulle les valeurs du spectre pour i<imin et i>imax, i de 0 a N-1
			int imin= 900;
			int imax=1800;
			Complex[] specfilt= new Complex[spectre.length];
			for (int i=0;i<spectre.length;i++) specfilt[i]=new Complex(0,0); 
			for (int i=imin;i<imax;i++) specfilt[i]=spectre[i];
			// pour imprimer avec gnuplot, on fera 
			// plot 'spectre.txt' using 2 with line, plot 'spectre.txt' using 3 
			FileWriter fw5 = new FileWriter("specfilt.txt");
			for(int i=0 ; i<spectre.length ; i++) {
				fw5.write((i*fe/spectre.length)+
						"\t"+specfilt[i].re()+"\t"+specfilt[i].im()+"\n");
			}
			fw5.close();

			// TFD inverse du spectre modifie specfilt puis demodulation : 
			// afin de recuperer l'enveloppe temporelle de cette composante
			// on multiplie par une exponentielle complexe pour decaler la frequence
			// de la composante filtree autour la frequence zero (cf. cours SSII)
			Complex[] x1 = FFT.ifft(specfilt);
			Complex[] env = new Complex[x1.length];
			int f0=440; //ou i0= 1320 environ
			FileWriter fw3 = new FileWriter("ifft.txt");
			for(int k=0 ; k< x1.length ; k++) {
				Complex expo = new Complex(Math.cos(2*Math.PI*f0*k/44100),
						-Math.sin(2*Math.PI*f0*k/44100));
				env[k]=expo.times(x1[k]); //x1[k].conjugate()) pour simple.wav
				fw3.write(k/fe+"\t"+x1[k].re()+"\t"+x1[k].im()+"\n");
			}
			fw3.close();

			// sauvegarde de l'enveloppe
			FileWriter fw4 = new FileWriter("env.txt");
			for(int i=0 ; i<spectre.length ; i++) {
				fw4.write((i*fe/env.length)+"\t"+env[i].re()+"\t"+env[i].im()+"\n");
			}
			fw4.close();          
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Erreur de creation de fichier");
		}
		//ici on joue le signal audio
		InputStream stream = new ByteArrayInputStream(samples);
		player.play(stream);
		System.exit(0);             
	}
}
