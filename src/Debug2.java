//package ?????;
import java.io.*;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
*
* @version 1.0
* @author Auzias & Genevier
*/
public class Debug2
{
   private int f;		      											//Frequence d'harmonique 0 de la note synthétisée
   private double[] tf = {1,2,3,4,5,6};
   private double[]  S = new double[(int)FE+1];					//TE*TO

  	//Static variables
	static final private double CH0[]	 = {1,2,3,4,5,6};
	static private double 	 FE = 44100.0;  						//Frequence d'échantillonnage
   static private double[] TPE = new double[(int)FE+1];		//Tableau de la periode d'échantillonnage
   static private double[][]  TO1 = new double[(int)FE+1][CH0.length];
   static private double[][]  T1 = new double[(int)FE+1][CH0.length];

	public Debug2(int f)
	{
	this.f=f;

	//Calcul de TPE[i] : temps discret
	this.tempsDiscret();
	System.out.println("temps discret calculé");
	//Calcul de TO[i] : ordonnée normalisées entre
	//[-0.99;0.99] du signal en fonction du temps
	this.synth();
	System.out.println("synth calculée");
	}

	public Debug2()	{this(1);}

	public void tempsDiscret()
	{
	//Calcul de TPE[i] : temps discret
	for(int i=0;i<FE;i++)
		TPE[i]=i/FE;
	}

	public void synth()
	{
	//Calcul de TO[i] : ordonnée du signal en fonction du temps
	for(int i=0;i<TPE.length;i++)
		{
			for(int j=0;j<CH0.length;j++)
				{
				T1[i][j]=CH0[j]*Math.sin(2*Math.PI*tf[j]*TPE[i]);
				}
		}
	for(int i=0;i<TPE.length;i++)
		{
		TO1[i][0]+=T1[i][0]+T1[i][1];
		TO1[i][1]+=T1[i][0]+T1[i][1]+T1[i][2];
		TO1[i][2]+=T1[i][0]+T1[i][1]+T1[i][2]+T1[i][3];
		TO1[i][3]+=T1[i][0]+T1[i][1]+T1[i][2]+T1[i][3]+T1[i][4];
		TO1[i][4]+=T1[i][0]+T1[i][1]+T1[i][2]+T1[i][3]+T1[i][4]+T1[i][5];
		TO1[i][5]+=T1[i][0]+T1[i][1]+T1[i][2]+T1[i][3]+T1[i][4]+T1[i][5];
		}

	int k=0;
	for(int j=0;j<CH0.length;j++,k++)
		{
		this.fileTable(T1,"harm"+k+".gplot",k);
		this.fileTable(TO1,"sum"+k+".gplot",k);
		}
	}

	static public void fileTable(double[][] array, String name,int j)
	{
	try 
		{
			FileWriter fw = new FileWriter(name);
	for(int i=0;i<TPE.length;i++)
				fw.write(TPE[i] + "\t" + array[i][j] + "\t" + (-1*(double)array[i][j]) + "\n");

			fw.close();
		}
	catch (Exception e) 
		{
			System.out.println("Erreur de création de fichier");
		}
	}

	public void toByteArray()
	{
	double[] samples = S;
	byte[] source = new byte[samples.length];
	for(int j = 0; j < samples.length; ++j) 
		source[j] = (byte) (samples[j] * 127);

	InputStream sound = new ByteArrayInputStream(source);
	play(sound);
	}

	public static void play(InputStream source) 
	{
	// définition d'un buffer de durée 100ms= 1/10s (soit fe*(B/8)/10)
	AudioFormat format = new AudioFormat((int)FE, 8, 1, false, true);
	int bufferSize = format.getFrameSize()*Math.round(format.getSampleRate()/10);
	byte[] buffer = new byte[bufferSize];
	SourceDataLine line;
	try
		{
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		line = (SourceDataLine) AudioSystem.getLine(info);
		line.open(format, bufferSize);
		}
	catch (LineUnavailableException e)
		{
		e.printStackTrace();
		return;
		}
	line.start();
	try {
		int numBytesRead = 0;
		while (numBytesRead != -1)
			{
			numBytesRead = source.read(buffer, 0, buffer.length);
			if (numBytesRead != -1)
				line.write(buffer, 0, numBytesRead);
			}
	}
	catch (IOException e)
		{
		e.printStackTrace();
		}
	line.drain();
	line.close();
	}

	public String printTable(double[] t)	{return this.printTable(t,false);}

	public String printTable(double[] t,boolean p)
	{
	String str="";
	for(int i=0;i<t.length;i++)
		{
		System.out.println(i + "\t\t" + t[i]);
		str += "" + i + "\t\t" + t[i] + "\n";
		}
	if(p)
		{
		System.out.println("Affichage :");
		System.out.println(str);
		}

	return str;
	}
}
