import java.io.*;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
* Class which creates a flute note with a frequence we can define. 
* As for the sampling rate, it is fixed at 44100 Hz.
* @version 1.0
* @author Auzias & Genevier
*/
public class Note
{
	private int f0;
	private double[] fh = new double[an.length];		//Tableau contenant les harmoniques de la fréquence
	private double[]  S = new double[(int)FE+1];
	private InputStream source;
	private AudioFormat format;
	private int bufferSize;
	private byte[] buffer;
	static final private double an[] = {1000,50,80,10,5,2,0.1,1};// {2,0.1,0.05,0.001,0.002,0.001}; Pour son d'orgue *mauvaise enveloppe*
	static private double 	FE = 44100.0;
	static private double[] tmpDscrt = new double[(int)FE+1];
	static private double[] sigSynth = new double[(int)FE+1];
	static private double[] TE = new double[(int)FE+1];

	/**
	*Constructor of Note
	*It uses the methods created below.
	*@param f Frequency of the note we want to create.
	*/
	public Note(int f0)
	{
	this.f0=f0;
	this.freqHarmoniques();
	this.tempsDiscret();
	this.synth();
	this.env();
	this.chronogramme();
	this.toByteArray();
	}

	/**
	*Default constructor
	*Here, the frequency is defined at 440 Hz.
	*/
	public Note()	{this(440);}

	/**
	*freqHarmonique method
	*Calculates the frequency for each harmonic from f0 = f and places the results in an array fh[] of the same size of CHO (array containing the coefficients of the harmonics) 
	*/
	public void freqHarmoniques()
	{
	for(int i=0;i<an.length;i++)
		fh[i]=(i+1)*f0;		// On pourrait faire fh[i] = fh[i-1]+f ??!!!??	(FASTER !)
	}

	/**
	*tempsDiscret method
	*Establishes an array containing the discrete times which has the form : 0, 1/FE, 2/FE, 3/FE, 4/FE ...
	*/
	public void tempsDiscret()
	{
	for(int i=0;i<FE;i++)
		tmpDscrt[i]=i/FE;
	}

	/**
	*synth method 
	*Calculates the ordinates of the signal for each discrete time (thus, there will be FE+1 values)
	*/
	public void synth()
	{
	for(int i=0;i<sigSynth.length;i++)
		{
			for(int j=0;j<fh.length;j++)
			{
				sigSynth[i]=sigSynth[i]+an[j]*Math.sin(2*Math.PI*fh[j]*tmpDscrt[i]);
		//Possible de le faire plus rapidement en utilisant la périodicité de la fonction sin !!!!!!!!   ?
			}
		}

	//Search for the absolute value of the maximum
	double max=0;
	for(int i=0;i<sigSynth.length-1;i++)
		{
		if(max<Math.max(Math.abs(sigSynth[i]),Math.abs(sigSynth[i+1])))
			max=Math.abs(Math.max(sigSynth[i],sigSynth[i+1]));
		}
		
	//Standardization of the ordinates between -0.99 and 0.99
	for(int i=0;i<sigSynth.length;i++)
		sigSynth[i] = (0.99*sigSynth[i])/max;

	this.fileWrite(sigSynth,"synth" + f0 + "Hz.gplot");
	}

	/**
	*env methode
	*Creates the envelope matching a flute note.
	*/
	public void env()
	{
	//Calculation of [0;FE/10] : 		y = 8*x + 0
	for(int i=0;i<(int)FE/10;i++)
			TE[i]=8*tmpDscrt[i];

 	//Calculation of [FE/10;2*FE/10] :	y = 2*x + 0.6
	for(int i=(int)FE/10;i<2*(int)FE/10;i++)
         TE[i]=2*tmpDscrt[i]+0.6;

	//Calculation of [2*FE/10;9*FE/10] :	y = 2*x/7 + 1.0571428
	for(int i=2*(int)FE/10;i<9*(int)FE/10;i++)
         TE[i]=-((2*tmpDscrt[i])/7)+1.0571428;

	//Calculation of [9*FE/10;FE] :		y = -8*x + 8
	for(int i=9*(int)FE/10;i<(int)FE;i++)
         TE[i]=-(8*tmpDscrt[i])+8;

 	this.fileWrite(TE,"env" + f0 + "Hz.gplot");
	}

	/**
	*chronogramme method
	*Calculates the digital timing diagram of the signal, by applying the envelope on the signal synth calculated before.
	*/
	public void chronogramme()
	{
	for(int i=0;i<S.length;i++)
		S[i]=sigSynth[i]*TE[i];

	this.fileWrite(S,"so" + f0 + "Hz.gplot");
	}

	/**
	*complexSignal method
	*Creates a array of Complex where the real parts are the ordinates of the signal and the imaginary parts are all null.
	*This array will allow to create an FFT.
	*/
	public Complex[] complexSignal()
	{
	Complex[] x = new Complex[S.length];
	for(int i=0;i<x.length;i++)
		x[i] = new Complex(S[i],0.0);

	return x;
	}

	/**
	*fileWrite method
	*Creates a file using a double array and the name (string) of the file
	*@param array
	*@param name of the file
	*/
	static public void fileWrite(double[] array, String name)
	{
	try 
		{
			FileWriter fw = new FileWriter(name);
			for(int i=0;i<array.length;i++) 
				fw.write(tmpDscrt[i] + "\t" + array[i] + "\t" + (-1*(double)array[i]) + "\n");

			fw.close();
		}
	catch (Exception e) 
		{
			System.out.println("Erreur de création de fichier");
		}
	}

	/**
	*toByteArray method
	*Create an byte array in order to allow the method "play" to play the sound
	*/
	public void toByteArray()
	{
	double[] samples = S;
	byte[] sound = new byte[samples.length];
	for(int j = 0; j < samples.length; ++j) 
		sound[j] = (byte) (samples[j] * 127);

	this.source = new ByteArrayInputStream(sound);
	// Creates a buffer during 100ms= 1/10s (or fe*(B/8)/10)
	this.format = new AudioFormat((float)44100.0, 8, 1, true, false);
	this.bufferSize = format.getFrameSize()*Math.round(format.getSampleRate()/10);
	this.buffer = new byte[bufferSize];
	}

	/**
	*play method
	*Plays the sound
	*/
	public void play() 
	{
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
	line.close();
	}
}
