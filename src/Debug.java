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
public class Debug
{
   private int f;		      											//Frequence d'harmonique 0 de la note synthétisée
   private double[] tf = {1,2,3,4,5,6};
   private double[]  S = new double[(int)FE+1];					//TE*TO

  	//Static variables
	static final private double CH0[]	 = {1,2,3,4,5,6};
	static private double 	 FE = 44100.0;  						//Frequence d'échantillonnage
   static private double[] TPE = new double[(int)FE+1];		//Tableau de la periode d'échantillonnage
   static private double[] TO1 = new double[(int)FE+1];
   static private double[] TO2 = new double[(int)FE+1];
   static private double[] TO3 = new double[(int)FE+1];
   static private double[] TO4 = new double[(int)FE+1];
   static private double[] TO5 = new double[(int)FE+1];
   static private double[] TO6 = new double[(int)FE+1];
   static private double[]  TE = new double[(int)FE+1];
   static private double[]  T1 = new double[(int)FE+1];
   static private double[]  T2 = new double[(int)FE+1];
   static private double[]  T3 = new double[(int)FE+1];
	static private double[]  T4 = new double[(int)FE+1];
	static private double[]  T5 = new double[(int)FE+1];
	static private double[]  T6 = new double[(int)FE+1];
	


	public Debug(int f)
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

	public Debug()	{this(1);}

	public void tempsDiscret()
	{
	//Calcul de TPE[i] : temps discret
	for(int i=0;i<FE;i++)
		TPE[i]=i/FE;
	}

	public void synth()
	{
	//Calcul de TO[i] : ordonnée du signal en fonction du temps
	for(int i=0;i<TO1.length;i++)
		{
			T1[i]=CH0[0]*Math.sin(2*Math.PI*tf[0]*TPE[i]);
			T2[i]=CH0[1]*Math.sin(2*Math.PI*tf[1]*TPE[i]);
			T3[i]=CH0[2]*Math.sin(2*Math.PI*tf[2]*TPE[i]);
			T4[i]=CH0[3]*Math.sin(2*Math.PI*tf[3]*TPE[i]);
			T5[i]=CH0[4]*Math.sin(2*Math.PI*tf[4]*TPE[i]);
			T6[i]=CH0[5]*Math.sin(2*Math.PI*tf[5]*TPE[i]);
			TO1[i]= T1[i]+T2[i];
			TO2[i]=TO1[i]+T3[i];
			TO3[i]=TO2[i]+T4[i];
			TO4[i]=TO3[i]+T5[i];
			TO5[i]=TO4[i]+T6[i];
		}
	this.fileWrite(T1,"harm1.gplot");
	this.fileWrite(T2,"harm2.gplot");
	this.fileWrite(T3,"harm3.gplot");
	this.fileWrite(T4,"harm4.gplot");
	this.fileWrite(T5,"harm5.gplot");
	this.fileWrite(T6,"harm6.gplot");
	this.fileWrite(TO1,"harm+12.gplot");	
	this.fileWrite(TO2,"harm+123.gplot");	
	this.fileWrite(TO3,"harm+1234.gplot");	
	this.fileWrite(TO4,"harm+12345.gplot");	
	this.fileWrite(TO5,"harm+123456.gplot");
	}

	static public void fileWrite(double[] array, String name)
	{
	try 
		{
			FileWriter fw = new FileWriter(name);
			for(int i=0;i<array.length;i++) 
				fw.write(TPE[i] + "\t" + array[i] + "\t" + (-1*(double)array[i]) + "\n");

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
