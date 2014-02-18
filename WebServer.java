/**
 * WebServer.java
 * 
 * This is a minimal working Web server to demonstrate
 * Java socket programming and simple HTTP interactions.
 * 
 * Author: Ooi Wei Tsang (ooiwt@comp.nus.edu.sg)
 */
import java.net.*;
import java.io.*;

class WebServer {

	// Configure the directory where all HTML files are 
	// stored.  You need to change this to your own local
	// directory if you want to play with this server code.
	static String WEB_ROOT = "/Users/zamakkat1/Dropbox/Study/Sem6/CS2105/Assignments/cs2105-a1";
	
	private static String runPerlScript(String scriptPath, String[] env) throws IOException {
		System.out.println("Executing Perl script: " + scriptPath);
		// Execute script with Environment variables given in String[] env
		Process todo = Runtime.getRuntime().exec("/usr/bin/perl " + scriptPath, env);
		
		// Get process output
	    BufferedReader reader = new BufferedReader(new InputStreamReader(todo.getInputStream()));
	    //BUG HERE WITH POST!!!!
	    String l = reader.readLine();
	    String output = "";
	    while (l!=null) {
	    	output += l + "\r\n";
	    	l = reader.readLine();
	    }
	    System.out.println(output);
		return output;
	}
	
	private static String error404(String item) {
		String output = "";
		String errorMessage = "I cannot find " + item + " on this server.\r\n";
		output += "HTTP/1.1 404 Not Found\r\n";
		output += "Content-length: " + errorMessage.length() + "\r\n\r\n";
		return output;
	}
	
	private static String error403(String item) {
		String output = "";
		String errorMessage = "You have no permission to access " + item + " on this server.\r\n";
		output += "HTTP/1.1 403 Forbidden\r\n";
		output += "Content-length: " + errorMessage.length() + "\r\n\r\n";
		return output;
	}
	
	private static String error400() {
		String output = "";
		String errorMessage = "400 Bad Request\r\n";
		output += "HTTP/1.1 400 Bad Request\r\n";
		output += "Content-length: " + errorMessage.length() + "\r\n\r\n";
		return output;
	}
	
	public static void main(String args[]) 
	{
		ServerSocket serverSocket;
		// Create a server socket, listening on port 2105.
		try 
		{
			serverSocket = new ServerSocket(2105);
		} 
		catch (IOException e)
		{
			System.err.println("Unable to listen on port 2105: " + e.getMessage());
			return;
		}

		// The server listens forever for new connections.  This
		// version handles only one connection at a time.
		while (true) 
		{
			Socket s;
			InputStream is;
			OutputStream os;
			BufferedReader br;
			DataOutputStream dos;

			// Wait for someone to connect.
			try 
			{
				s = serverSocket.accept();
			} 
			catch (IOException e)
			{
				System.err.println("Unable to accept connection: " + e.getMessage());
				continue;
			}
			System.out.println("Connection accepted.");
			
			// Get the input stream (to read from) and output stream
			// (to write to), and wrap nice reader/writer classes around
			// the streams.
			try 
			{
				is = s.getInputStream();
				br = new BufferedReader(new InputStreamReader(is));

				os = s.getOutputStream();
				dos = new DataOutputStream(os);

				// Now, we wait for HTTP request from the connection
				String line = br.readLine();

				// Bail out if line is null. In case some client tries to be 
				// funny and close immediately after connection.  (I am
				// looking at you, Chrome!)
				if (line == null)
				{
					continue;
				}
				
				// We are expecting the first line to be GET <filename> ...
				// We only care about the first two tokens here.
				String tokens[] = line.split(" ");
				
				String scriptPath = "";
				String scriptName = "";
				String req_method = "";
				String query_string = "";
				String content_type = "";
				String content_length = "";
				
				if (tokens[0].equals("GET")) {					
					// We do not really care about the rest of the HTTP
					// request header either.  Read them off the input
					// and throw them away.
					while (!line.equals("")) {
						line = br.readLine();
					}

					int argIndex = tokens[1].indexOf('?');
					if (argIndex == -1) {
						scriptName = tokens[1];
						query_string = "";
					} else {
						scriptName = tokens[1].substring(0, argIndex);
						query_string = tokens[1].substring(argIndex+1);
					}
					scriptPath = WEB_ROOT + scriptName;

					System.out.println("GET" + scriptName);
					req_method = "GET";

					File file = new File(scriptPath);
					
					// Check for file permission or not found error.
					if (!file.exists() || !file.isFile()) {
						String error = error404(scriptName);
						dos.writeBytes(error);
						s.close();
						continue;
					} else if (!file.canRead()) {
						String error = error403(scriptName);
						dos.writeBytes(error);
						s.close();
						continue;
					}
					
					// Assume everything is OK then.  Send back a reply.
					dos.writeBytes("HTTP/1.1 200 OK\r\n");

					if (scriptName.endsWith(".pl")) 
					{
						String[] env = {"REQUEST_METHOD="+req_method, "QUERY_STRING="+query_string};
						String todo = runPerlScript(scriptPath, env);
						
					    dos.writeBytes("Content-length: " + (todo.length()-47) + "\r\n");
						dos.writeBytes(todo);
						
					} else {
						// We send back some HTTP response headers.
						dos.writeBytes("Content-length: " + file.length() + "\r\n");
	
						// We could have use Files.probeContentType to find 
						// the content type of the requested file, but let 
						// me do the poor man approach here.
						if (scriptName.endsWith(".html")) 
						{
							dos.writeBytes("Content-type: text/html\r\n");
						}
						if (scriptName.endsWith(".jpg")) 
						{
							dos.writeBytes("Content-type: image/jpeg\r\n");
						}
						
						dos.writeBytes("\r\n");
						
						// Finish with HTTP response header.  Now send
						// the body of the file.
						
						// Read the content 1KB at a time.
						byte[] buffer = new byte[1024];
						FileInputStream fis = new FileInputStream(file);
						int size = fis.read(buffer);
						while (size > 0) 
						{
							dos.write(buffer, 0, size);
							size = fis.read(buffer);
						}
					}
					
					dos.flush();
					// Finally, close the socket and get ready for
					// another connection.
					s.close();
					continue;
					
				} else if (tokens[0].equals("POST")) {
					
					scriptName = tokens[1];
					scriptPath = WEB_ROOT + scriptName;
					
					System.out.println("POST " + scriptName);
					
					while (!line.equals("")) {
						if (line.contains("Content-Type: ")) {
							content_type = (String) line.substring("Content-Type: ".length());
							
						} else if (line.contains("Content-Length: ")) {
							content_length = (String) line.substring("Content-Length: ".length());
							
						}
						System.out.println(line);
						line = br.readLine();
					}
					int ch = br.read();
					
					String scriptInput = "" + (char) ch;
					while (br.ready()) {
						ch = br.read();
						scriptInput += (char) ch;
					}

					req_method = "POST";
					System.out.println(content_type);
					System.out.println(content_length);
					
					System.out.println(scriptInput);
					
					String[] env = {"REQUEST_METHOD="+req_method, "CONTENT_TYPE="+content_type, "CONTENT_LENGTH="+content_length};
					String todo = runPerlScript(scriptPath, env);
				    
					System.out.println("hello");
					dos.writeBytes("Content-length: " + todo.length() + "\r\n");
					dos.writeBytes(todo);
					dos.flush();
					
					s.close();
					continue;
				} else {
					String error = error400();
					dos.writeBytes(error);
					s.close();
					continue;
				}

				
				
			}
			catch (IOException e)
			{
				System.err.println("Unable to read/write: "  + e.getMessage());
			}
		}
	}
}
