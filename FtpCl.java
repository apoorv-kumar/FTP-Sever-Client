/*
 * PRIMARY CLIENT CODE
 * 
 * @AUTHOR - Apoorv Kumar , 09010111 , IIT-Guwahati
 * 
 */

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author apoorv
 */
public class FtpCl {

	private String current_local_dir;

	public FtpCl()
	{
		current_local_dir = System.getProperty("user.home");
	}

	private void readFile(String file_name ,int serv_port , InetAddress srv_addr) throws IOException
	{

		Socket sock = new Socket(srv_addr, serv_port);
		InputStream is = null;
		FileOutputStream fos = null;

		byte[] mybytearray = new byte[1024];
		try {
			is = sock.getInputStream();
			fos = new FileOutputStream(current_local_dir + "/" +file_name);
			//chk what is diff in mget and get stuff.
			
			int count;
			while ((count = is.read(mybytearray)) >= 0) {
				fos.write(mybytearray, 0, count);
			}
		} finally {
			if (!(fos == null)) fos.close();;
			
			sock.close();
		}
		System.out.println("get completed successfully.");
	}
	
	private void writeFile(String file_name , int serv_port , InetAddress srv_addr) throws  IOException
	{
							
		//open file
		File myFile = new File(current_local_dir+"/" +file_name);
		FileInputStream fis = null;

		//write on the new port
		Socket sock = new Socket(srv_addr, serv_port);
		OutputStream os = null;
		try {		
			byte[] mybytearray = new byte[1024];
			fis = new FileInputStream(myFile);
			os = sock.getOutputStream();

			int count;
			while ((count = fis.read(mybytearray)) >= 0) {
				os.write(mybytearray, 0, count);
			}
			os.flush();
		}catch(FileNotFoundException e)
		{
			System.err.println("couldn't open file");
		} finally
		{
			sock.close();
		}

	}
	// -------------        interface functions      --------------------------------------

	//'socket' is the control socket
	public void getFile(String file_name ,Socket socket) throws IOException
	{
		System.out.println("Initiating GetFile...");
		//send request string to server

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		//create req string and send
		String str = "GET:" + file_name;
		os.write(str.getBytes());

		//rcv the new port number
		DataInputStream dis = new DataInputStream(is);
		int port_num = dis.readInt();

		//read on the port opened by server
		InetAddress srv_address = InetAddress.getByName("localhost");
		readFile(file_name, port_num, srv_address);

	}

	//'socket' is the control socket
	public void putFile(String file_name ,Socket socket) throws IOException
	{
		System.out.println("Initiating PutFile...");
		//send request string to server

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		//create req string and send
		String str = "PUT:" + file_name;
		os.write(str.getBytes());

		//rcv the new port number
		DataInputStream dis = new DataInputStream(is);
		int port_num = dis.readInt();

		//read on the port opened by server
		InetAddress srv_address = InetAddress.getByName("localhost");
		
		writeFile(file_name, port_num, srv_address);

	}

	//'socket' is the control socket
	//TODO - redo the entire thing .... with demarcaters
	public void mGet(String f_list , Socket socket) throws IOException
	{
		System.out.println("Initiating mGet..."  );
		String[] file_list = f_list.split(":");
		//send request string to server

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		for( int i =0 ; i < file_list.length ; i++)
		{
			String file_name = file_list[i];
			//create req string and send
			String str = "MGET:" + file_name;
			os.write(str.getBytes());

			//rcv the new port number
			DataInputStream dis = new DataInputStream(is);
			int port_num = dis.readInt();

			//read on the port opened by server
			InetAddress srv_address = InetAddress.getByName("localhost");
			readFile(file_name, port_num, srv_address);
		}

	}

	//'socket' is the control socket
	//TODO - redo the entire thing .... without the hack (though the hack is more efficient :P )
	public void getFolderR(String folder_name ,Socket socket) throws IOException
	{
		System.out.println("Initiating GetFolder Recursive...");
		//send request string to server

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		//create req string and send
		String str = "GETR:" + folder_name;

		os.write(str.getBytes());

		//rcv the new port number
		DataInputStream dis = new DataInputStream(is);
		int port_num = dis.readInt();

		//read on the port opened by server
		String file_name = "."+folder_name+".tar.gz";
		InetAddress srv_address = InetAddress.getByName("localhost");
		readFile(file_name, port_num, srv_address);


		//system call to put things in place :)
		//System.out.println("unzip exec: tar -xzvf " +current_local_dir +"/"+file_name );
		String command_unzip = "tar -xzvf " +current_local_dir +"/"+file_name + " -C " + current_local_dir;
		String command_remove = "rm " + current_local_dir +"/"+file_name;

		Runtime.getRuntime().exec( command_unzip);
		try {// TODO - this is just a hack !
			Thread.sleep(1000); // assuming it would be sufficient
		} catch (Exception e) {
		}
		Runtime.getRuntime().exec(command_remove);


	}


	//'socket' is the control socket
	public void putFolderR(String folder_name ,Socket socket) throws IOException
	{
		System.out.println("Initiating recursive put folder...");
		//send request string to server

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		//create req string and send
		String str = "PUTR:" + folder_name;
		os.write(str.getBytes());

		//rcv the new port number
		DataInputStream dis = new DataInputStream(is);
		int port_num = dis.readInt();

		//read on the port opened by server
		InetAddress srv_address = InetAddress.getByName("localhost");
		String file_name = "." + folder_name +".tar.gz";
		System.out.println("zip exec: tar -zcvf " +current_local_dir +"/"+file_name + " "+ current_local_dir +"/" +folder_name);
		Runtime.getRuntime().exec("tar -zcvf " +current_local_dir +"/"+file_name +" " +current_local_dir +"/" +folder_name );
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			Logger.getLogger(FtpCl.class.getName()).log(Level.SEVERE, null, ex);
		}

		writeFile(file_name, port_num, srv_address);

	}


	//'socket' is the control socket
	public void mGetStar(Socket socket) throws  IOException
	{
		System.out.println("Initiating MGET:*...");

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		//create req string and send
		String str = "MGET:*";
		os.write(str.getBytes());

		//get files from input stream
		BufferedReader bris = new BufferedReader(new InputStreamReader( is));


		String file_name;
		ArrayList<String> file_list = new ArrayList<String>();
		while ( (file_name = bris.readLine()) != null) {
			if(file_name.equals("--terminate-line--") ) break;
			if( !file_name.endsWith("/"))file_list.add(file_name);
			if( !file_name.endsWith("/")) System.out.println(file_name);
		}

		//apply mget on all of the files
		for (int i = 0; i < file_list.size(); i++) {
			System.out.println("requesting : " + file_list.get(i));
			mGet(file_list.get(i), socket);
		}

	}


	//'socket' is the control socket
	public void mPutStar(Socket socket) throws  IOException
	{
		System.out.println("Initiating MPUT:*...");

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		//create req string and send
		String str = "MPUT:*";
		os.write(str.getBytes());

		//get files from input stream
		Runtime r = Runtime.getRuntime();
		Process p = r.exec("ls -p1 " + current_local_dir ); // returns only files , 1 in each line
		 System.out.println("exec : ls -p1 " + current_local_dir );
		InputStream in = p.getInputStream();
		 BufferedInputStream buf = new BufferedInputStream(in);
		InputStreamReader inread = new InputStreamReader(buf);
		BufferedReader bris = new BufferedReader(inread);

		String file_name;
		ArrayList<String> file_list = new ArrayList<String>();
		while ( (file_name = bris.readLine()) != null) {
			//hack to differentiate file from folder , the -p in ls system call adds / to end of each folder (only)
			if( !file_name.endsWith("/")) file_list.add(file_name);
			if( !file_name.endsWith("/")) System.out.println(file_name);
		}

		//apply mput on all of the files
		for (int i = 0; i < file_list.size(); i++) {
			mPut(file_list.get(i), socket);
		}

	}

	//'socket' is the control socket
	//TODO - redo the entire thing .... with demarcaters
	public void mPut(String f_list , Socket socket) throws IOException
	{

		String[] file_list = f_list.split(":");
		//send request string to server

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		for( int i =0 ; i < file_list.length ; i++)
		{
		String file_name = file_list[i];
		//get stream

		//create req string and send
		String str = "MPUT:" + file_name;
		os.write(str.getBytes());

		//rcv the new port number
		DataInputStream dis = new DataInputStream(is);
		int port_num = dis.readInt();

		//read on the port opened by server
		InetAddress srv_address = InetAddress.getByName("localhost");

		writeFile(file_name, port_num, srv_address);
		}

	}


	//'socket' is the control socket
	public void getDirList(Socket socket) throws  IOException
	{
		System.out.println("Initiating DIR...");

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		//create req string and send
		String str = "DIR:";
		os.write(str.getBytes());

		//rcv the directory listing
		DataInputStream dis = new DataInputStream(is);
		byte b[] = new byte[20480];//restriction - 10240 chars limit - 10K chars
		int length = dis.read(b);

		//splice the useful part
		byte[] b_useful = Arrays.copyOf(b, length);

		//convert to string and print
		System.out.println(new String(b_useful) );

	}

	public void getLocalDirList() throws  IOException
	{

		Runtime r = Runtime.getRuntime();

		//run a system call

		Process p = r.exec("ls -l " + current_local_dir);
		System.out.println("PROCESSING REQUEST: ls -l " + current_local_dir);
		 InputStream in = p.getInputStream();
		 BufferedInputStream buf = new BufferedInputStream(in);
		InputStreamReader inread = new InputStreamReader(buf);
		BufferedReader bufferedreader = new BufferedReader(inread);

		System.out.println("Current Local Dir: " + current_local_dir);

		String str;
		while( (str = bufferedreader.readLine()) != null)
		{
			System.out.println(str);
		}

	}

	public void changeDir(Socket socket , String new_dir) throws IOException
	{
		System.out.println("requesting dir change to " + new_dir);

		//get stream
		OutputStream os = socket.getOutputStream();
		InputStream is = socket.getInputStream();

		//create req string and send
		String str = "CD:" + new_dir;
		os.write(str.getBytes());

		//rcv the directory listing
		DataInputStream dis = new DataInputStream(is);
		byte b[] = new byte[256];//restriction - 128char limit
		int length = dis.read(b);

		//splice the useful part
		byte[] b_useful = Arrays.copyOf(b, length);

		//convert to string and print
		System.out.println(new String(b_useful) );

	}


	public void changeLocalDir(String lDir)
	{
		current_local_dir = lDir;
		System.out.println("Local directory changed to: " + current_local_dir);
	}

	public void init() throws Exception {
		InetAddress addr = InetAddress.getByName("localhost");
		int port = 20021;

		Socket socket = new Socket(addr, port);
		
		System.out.println("WELCOME - CLIENT INTERFACE");
		
		String command = "BEGIN";
		BufferedReader user_input = new BufferedReader( new InputStreamReader(System.in) );


		//get string from user , parse and process
	while (true)
	{
		System.out.print("=>");
		command = user_input.readLine();
		//retrieve CODE
		String partioned_req[] = command.split(":" , 2);
		String code = partioned_req[0];

		if( code.equals( "CD") )
		{
			String args = partioned_req[1];
			changeDir(socket, args);
		}
		else if ( code.equals( "LCD"))
		{
			String args = partioned_req[1];
			changeLocalDir(args);
		}
		else if( code.equals( "DIR"))
		{
			getDirList(socket);
		}
		else if( code.equals( "LDIR"))
		{
			getLocalDirList();
		}
		else if(code.equals("GET"))
		{
			String args = partioned_req[1];
			getFile(args, socket);
		}
		else if(code.equals("PUT"))
		{
			String args = partioned_req[1];
			putFile(args, socket);
		}
		else if(code.equals( "RGET"))
		{
			String args = partioned_req[1];
			getFolderR(args, socket);
		}
		else if( code.equals("RPUT"))
		{
			String args = partioned_req[1];
			putFolderR(args, socket);
		}
		else if( code.equals("MGET"))
		{
			String args = partioned_req[1];
			if(args.equals("*"))
			{
				mGetStar(socket);
			}
			else{
				mGet(args, socket);
			}
		}

		else if( code.equals("MPUT"))
		{
			String args = partioned_req[1];
			if(args.equals("*"))
			{
				mPutStar(socket);
			}
			else{
				mPut(args, socket);
			}
		}
		else if(code.equals("EXIT"))
		{
			break;
		}

		else
		{
			System.err.println("INVALID CODE: " + code);
		}
	}

		//exit procedure
		socket.close();

	}

	public static void main(String[] args) throws IOException, Exception {
		// TODO code application logic here
			FtpCl fc = new FtpCl();
			fc.init();



	}


}



