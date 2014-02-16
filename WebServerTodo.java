import java.io.*;
import java.net.*;

public class WebServerTodo {

	static String WEB_ROOT = "/Users/zweedaothaiduy/Dropbox/Modules/Sem 10/CS2105 COMPUTER NETWORKING/Assignment/cs2105-a1/";
	
	public static void main(String[] args) throws Exception 
	{
		ServerSocket myServerSocket;
		myServerSocket = new ServerSocket(2105);
		
		while (true)
		{
			Socket s;
			InputStream is;
			OutputStream os;
			BufferedReader br;
			DataOutputStream dos;
			
			//setup socket and input, output stream
			s=myServerSocket.accept();
			System.out.println("Connection accepted!");
			
			is = s.getInputStream();
			br = new BufferedReader(new InputStreamReader(is));
			
			os = s.getOutputStream();
			dos = new DataOutputStream(os);
			
			//read lines from http message from client
			String line = br.readLine();
			if (line == null)
			{
				continue;
			}
			
			String tokens[] = line.split(" ");
			
			//run todo.pl
			Process todoProcess = Runtime.getRuntime().exec("/usr/bin/perl " + WEB_ROOT + tokens[1]);
			
			//setup input, output stream for todo.pl
			InputStream todoIS;
			BufferedReader todoBR;
			OutputStream todoOS;
			DataOutputStream todoDOS;

			todoIS = todoProcess.getInputStream();
			System.out.println(todoIS);
			todoBR = new BufferedReader(new InputStreamReader(todoIS));
			System.out.println(todoBR);
			
			todoOS = todoProcess.getOutputStream();
			todoDOS = new DataOutputStream(todoOS);
			
			String todoLine;
			todoLine = todoBR.readLine();
			System.out.println(todoLine);
			/*while (!todoLine.equals(""))
			{
				System.out.println(todoLine);
				todoLine = todoBR.readLine();
			}
			*/
			/*while (!line.equals(""))
			{
				System.out.println(line);
				line = br.readLine();
			}
			*/
			
			//close socket
			s.close();
		}
		
	}

}
