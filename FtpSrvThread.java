/*
 * THREAD USED BY SERVER
 * @AUTHOR - Apoorv Kumar , 09010111 , IIT-Guwahati
 * 
 */



import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author apoorv
 */
public class FtpSrvThread implements Runnable{

	private Socket srvSoc;
	private String current_dir;

	public FtpSrvThread( Socket sc,  String cd ) {
		srvSoc = sc;
		current_dir = cd;
	}


	public void run() {

			try{
			//listen on given port
			while(true)
			{
			InputStream is = srvSoc.getInputStream();

			//process request
			//rcv the directory listing

			byte b[] = new byte[512];//restriction - 256 chars limit

			int length = is.read(b);

			if(length == -1) // the client has closed connection
			{
				System.out.println("Connection terminated by the client\nServer thread exiting..."); // ABNORMALLY TERMINATED
				srvSoc.close();
				break;
			}
			//splice the useful part
			byte[] b_useful = Arrays.copyOf(b, length);

			//convert to string and print
			String request =  new String(b_useful);

			System.out.println("Received - " + request);

			//process
			process(request);

			//close resources
			if(request.equals("EXIT"))
			{
				System.out.println("Exit request by client : " + srvSoc.getInetAddress().toString());
				srvSoc.close();
				break;
			}

			}
			} catch (InterruptedException ex) {
			Logger.getLogger(FtpSrvThread.class.getName()).log(Level.SEVERE, null, ex);
		}catch (IOException e) {
				System.out.println(e);
				System.exit(-1);
			}




	}


	public void process(String request) throws IOException, InterruptedException
	{
		/*
		 * request format is
		 *	<CODE>:<ARGUMENTS>: ...
		 */
		System.out.println("analysing request...");
		//retrieve CODE
		String partioned_req[] = request.split(":" , 2);

		String request_code = partioned_req[0];
		request_code = request_code.replaceAll("\\s","");
		System.out.println("request code: " + request_code);
		//get out stream of already established ctrl link
		OutputStream os = srvSoc.getOutputStream();

		// ----------------- GET -----------------------
		if(request_code.equals( "GET" ) )
		{
			System.out.println("processing get...");
			String file_name = partioned_req[1];

			//send the file on some socket
			ServerSocket servsock = new ServerSocket();
			servsock.bind(null);
			int s_port = servsock.getLocalPort();

			//send the port num on socket

			DataOutputStream dos = new DataOutputStream(os);
			dos.writeInt(s_port);

			//open file
			File myFile = new File(current_dir+"/" +file_name);
			FileInputStream fis = null;


			//write on the new port
			//start listening on new port
			Socket sock = servsock.accept();
			try {
				byte[] mybytearray = new byte[1024];
				fis = new FileInputStream(myFile);
				os = sock.getOutputStream();

				int count;
				//start writing on new port
				while ((count = fis.read(mybytearray)) >= 0) {
					os.write(mybytearray, 0, count);
				}
				System.out.println("writing on the port complete");
				os.flush();
			}finally
			{
				sock.close();
			}
		}

		// \ -----------------------------------------------

		// ----------------- MGET -----------------------
		//TODO - redo this with demarcaters
		if(request_code.equals( "MGET" ) )
		{

			String file_name = partioned_req[1];
			System.out.println("processing MGET file :" +file_name );

			if(file_name.equals("*"))
			{
				System.out.println("PROCESSING REQUEST: ls -l " + current_dir);
				Runtime r = Runtime.getRuntime();

				//run a system call

				Process p = r.exec("ls -p1 " + current_dir ); // returns only files , 1 in each line
				System.out.println("exec : ls -p1 " + current_dir);
				 PrintWriter out = new PrintWriter(os, true);

				InputStream in = p.getInputStream();
				 BufferedInputStream buf = new BufferedInputStream(in);
				InputStreamReader inread = new InputStreamReader(buf);
				BufferedReader bufferedreader = new BufferedReader(inread);

				// Read the ls output

				
				String str;
				while( (str = bufferedreader.readLine()) != null)
				{
					out.println(str);
				}

				out.println("--terminate-line--"); // this acts as the demarcater

				
//				String result;
//				char [] cbuf = new char[10240]; // restriction - the listing can be 10240 chars long at max
//				int size;
//				size = bufferedreader.read(cbuf);
//				result = String.copyValueOf(cbuf, 0, size);
//				//result has the output of system call
//				//return it on ctrl link
//				System.out.println("RETURNING RESULT OF DIR");
//				os.write(result.getBytes());
//				os.write();
			}
			else{
				//send the file on some socket
				ServerSocket servsock = new ServerSocket();
				servsock.bind(null);
				int s_port = servsock.getLocalPort();

				//send the port num on socket

				DataOutputStream dos = new DataOutputStream(os);
				dos.writeInt(s_port);

				//open file
				File myFile = new File(current_dir+"/" +file_name);
				FileInputStream fis = null;


				//start listening on new port
				Socket sock = servsock.accept();
				try {
					byte[] mybytearray = new byte[1024];
					fis = new FileInputStream(myFile);
					os = sock.getOutputStream();

					int count;
					//write on the new port
					while ((count = fis.read(mybytearray)) >= 0) {
						os.write(mybytearray, 0, count);
					}
					System.out.println(file_name + " sent successfully ...");
					os.flush();
				}finally
				{
					sock.close();
				}

			}
		}

		// \ -----------------------------------------------



		// ----------------- PUT -----------------------
		if(request_code.equals( "PUT" ) )
		{
			System.out.println("processing put...");
			String file_name = partioned_req[1];

			//send the file on some socket
			ServerSocket servsock = new ServerSocket();
			servsock.bind(null); // sever listening on localhost - TODO - generalize this
			int s_port = servsock.getLocalPort();

			//send the port num on socket
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeInt(s_port);

			//listen on the new port
			Socket sock = servsock.accept();
			InputStream is = null;
			FileOutputStream fos = null;

			//read file from client
			byte[] mybytearray = new byte[1024];
			try {
				is = sock.getInputStream();
				fos = new FileOutputStream(current_dir + "/" +file_name);

				int count;
				while ((count = is.read(mybytearray)) >= 0) {
					fos.write(mybytearray, 0, count);
				}
			} finally {
				//close connections
				fos.close();
				is.close();
				sock.close();
			}
		}

		// \ -----------------------------------------------


		// ----------------- MPUT -----------------------
		if(request_code.equals( "MPUT" ) )
		{
			String file_name = partioned_req[1];
			System.out.println("MPUT: processing file : " + file_name);

			if(file_name.equals("*"))
			{
				//expect mputs

			}
			else{
				//send the file on some socket
				ServerSocket servsock = new ServerSocket();
				servsock.bind(null); // sever listening on localhost - TODO - generalize this
				int s_port = servsock.getLocalPort();

				//send the port num on socket
				DataOutputStream dos = new DataOutputStream(os);
				dos.writeInt(s_port);

				//listen on the new port
				Socket sock = servsock.accept();
				InputStream is = null;
				FileOutputStream fos = null;

				//read file from client
				byte[] mybytearray = new byte[1024];
				try {
					is = sock.getInputStream();
					fos = new FileOutputStream(current_dir + "/" +file_name);

					int count;
					while ((count = is.read(mybytearray)) >= 0) {
						fos.write(mybytearray, 0, count);
					}
				} finally {
					//close connections
					fos.close();
					is.close();
					sock.close();
				}

			}
		}

		// \ -----------------------------------------------

		// ----------------- GETR -----------------------
		if(request_code.equals( "GETR" ) )
		{
			System.out.println("processing get recursive request...");
			String folder_name = partioned_req[1];
			
			File wd = new File(current_dir);

			String file_name = "." + folder_name +".tar.gz";
			//enumerate hack
			System.out.println("recursively enumerating files on folder: " + folder_name);
			//System.out.println("executing : tar -zcvf " +current_dir +"/."+folder_name+ ".tar.gz " + current_dir +"/" +folder_name );
			Runtime.getRuntime().exec("tar -zcvf " +current_dir +"/"+file_name+ " " + folder_name  ,null, wd );

			Thread.sleep(1000);

			//send the file on some socket
			ServerSocket servsock = new ServerSocket();
			servsock.bind(null);
			int s_port = servsock.getLocalPort();

			//send the port num on socket

			DataOutputStream dos = new DataOutputStream(os);
			dos.writeInt(s_port);

			//open file
			String file_addr = current_dir+"/" +file_name;
			File myFile = new File(current_dir+"/" +file_name);
			if(!myFile.canRead()) System.err.println("file not available");
			FileInputStream fis = null;


			//write on the new port
			//start listening on new port
			Socket sock = servsock.accept();
			try {
				byte[] mybytearray = new byte[1024];
				fis = new FileInputStream(myFile);
				os = sock.getOutputStream();

				int count;
				//start writing on new port
				while ((count = fis.read(mybytearray)) >= 0) {
					os.write(mybytearray, 0, count);
				}
				System.out.println("writing on the port complete");
				os.flush();
			}catch(FileNotFoundException e)
			{
				System.err.println(e.toString());
			} finally
			{
				sock.close();
			}
		}

		// \ -----------------------------------------------



		// ----------------- PUTR -----------------------
		if(request_code.equals( "PUTR" ) )
		{
			System.out.println("processing recursive put request...");
			String folder_name = partioned_req[1];

			//send the file on some socket
			ServerSocket servsock = new ServerSocket();
			servsock.bind(null); // sever listening on localhost - TODO - generalize this
			int s_port = servsock.getLocalPort();

			//send the port num on socket
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeInt(s_port);

			//listen on the new port
			Socket sock = servsock.accept();
			InputStream is = null;
			FileOutputStream fos = null;

			String file_name = "." + folder_name+".tar.gz";

			//read file from client
			byte[] mybytearray = new byte[1024];
			try {
				is = sock.getInputStream();
				fos = new FileOutputStream(current_dir + "/" +file_name);

				int count;
				while ((count = is.read(mybytearray)) >= 0) {
					fos.write(mybytearray, 0, count);
				}
			} finally {
				//close connections
				fos.close();
				is.close();
				sock.close();
			}

			//clean up the mess
			String command_unzip = "tar -xzvf " +current_dir +"/"+file_name + " -C " + current_dir;
			String command_remove = "rm " + current_dir +"/"+file_name;
			Runtime.getRuntime().exec( command_unzip);
			try {// TODO - this is just a hack !
				Thread.sleep(1000); // assuming it would be sufficient
			} catch (Exception e) {
			}
			Runtime.getRuntime().exec(command_remove);

		}

		// \ -----------------------------------------------


		// ----------------- DIR -------------------------
		else if( request_code.equals("DIR") )
		{
			System.out.println("PROCESSING REQUEST: ls -l " + current_dir);
			Runtime r = Runtime.getRuntime();

			//run a system call

			Process p = r.exec("ls -l " + current_dir);
			 InputStream in = p.getInputStream();
			 BufferedInputStream buf = new BufferedInputStream(in);
			InputStreamReader inread = new InputStreamReader(buf);
			BufferedReader bufferedreader = new BufferedReader(inread);

			// Read the ls output
			//TODO - find a more efficient procedure
			String result;
			char [] cbuf = new char[10240]; // restriction - the listing can be 10240 chars long at max
			int size;
			size = bufferedreader.read(cbuf);
			result = String.copyValueOf(cbuf, 0, size);
			result = "Current Dir: " + current_dir + "\n" + result;
			//result has the output of system call
			//return it on ctrl link
			System.out.println("RETURNING RESULT OF DIR");
			os.write(result.getBytes());
		}



		// \ -----------------------------------------------






		// \ -----------------------------------------------



		// ----------------- CD DIR -------------------------
		else if( request_code.equals("CD") )
		{
			String new_dir =  partioned_req[1];
			System.out.println("changing directory to : " + new_dir);

			current_dir = new_dir;
			String result = "Dir change successful . New dir : " + current_dir;
			//result has the output of system call
			//return it on ctrl link
			System.out.println("RETURNING RESULT OF DIR");
			os.write(result.getBytes());
		}
		// \ -----------------------------------------------
	}




}
