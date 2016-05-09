/*
 * PRIMARY SERVER CODE
 * 
@AUTHOR - Apoorv Kumar , 09010111 , IIT-Guwahati
 */


import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
/**
 *
 * @author apoorv
 */
public class FtpSrv {


	public void  start() throws IOException
	{
		//initialize the data port - not needed in passive
		//ServerSocket dataSoc = new ServerSocket(20);
		System.out.println("WELCOME - SERVER INTERFACE");
		
		//initialize the ctrl port
		ServerSocket ctrlSoc = new ServerSocket(20021);
		System.out.println("Listening on socket 20021...");
		
		//listen on both ports

		while(true)
		{
			Socket sc_req = ctrlSoc.accept();
			System.out.println("New connection established ...");
			FtpSrvThread new_cl = new FtpSrvThread(sc_req, System.getProperty("user.home"));
			new_cl.run();
		}
	}


	public static void main(String[] args) throws IOException, Exception {
		// TODO code application logic here
		FtpSrv fs = new FtpSrv();
		fs.start();
		//


	}
}
