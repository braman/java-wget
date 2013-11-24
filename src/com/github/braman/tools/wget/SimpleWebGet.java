package com.github.braman.tools.wget;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleWebGet {


	public SimpleWebGet(String webUrl) {
		boolean	ok			= true;
		String 	fileName 	= webUrl.substring(webUrl.lastIndexOf('/')+1);

		URL 		url 	= null;
		InetAddress address = null;
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

		System.out.printf("--%8s--\t%s\n%12s>\t`%s`\n", sdf.format(new Date()), webUrl, "=", fileName);

		try {
			url 	= new URL(webUrl);
			address = InetAddress.getByName(url.getHost());
		} catch (MalformedURLException e) {
			ok = false;
		} catch (UnknownHostException e) {
			ok = false;
		}

		String ip 		= ok ? address.getHostAddress() : "Host Not Found";
		String hostName = getUrlDomainName(webUrl);
		String port		= (url != null && url.getPort() > 0) ? Integer.toString(url.getPort()) : "80";

		System.out.printf("Resolving %s... %s\n", hostName, ip);


		if (!ok) {
			return;
		}

		HttpURLConnection 	connection 	= null;


		int 	responseCode 	= -1;
		String 	responseMessage = "";

		try {
			connection 		= (HttpURLConnection) url.openConnection();
			connection.connect();
			responseCode 	= connection.getResponseCode();
			responseMessage = connection.getResponseMessage();
		} catch (IOException e) {
			ok = false;
		}

		System.out.printf("Connecting to %s[%s]:%s... %s.\n", hostName, ip, port, (ok ? "connected" : "failed"));

		if (!ok) {
			return;
		}

		ok = responseCode == 200;

		System.out.printf("HTTP request sent, awaiting response... %d %s\n", responseCode, responseMessage);

		if (!ok) {
			System.out.printf("%s ERROR %d: %s.\n", sdf.format(new Date()), responseCode, responseMessage);
			return;
		}

		int contentLength 	= connection.getContentLength();
		String contentType 	= connection.getContentType();
		int alreadyReadBytes	= 0;
		int count				= 0;

		float	avgSpeed	= 0.f;

		if (contentLength > 0) {
			System.out.printf("Length: %,d [%s]\n", contentLength, contentType);
		} else {
			System.out.printf("Length: %s [%s]\n", "?", contentType);
		}

		try {
			InputStream in 			= connection.getInputStream();
			OutputStream fileOutput = null;
			OutputStream bufferedOut = null;

			try {
				fileOutput = new FileOutputStream(fileName);
				bufferedOut = new BufferedOutputStream(fileOutput, 1024);

				byte data[] = new byte[1024];
				boolean downloadComplete = false;

				final int width = 100; // progress bar width in chars

				final long pico = 1000000L;

				while (!downloadComplete) {
					long timeStart = System.nanoTime();
					int  readBytes = in.read(data, 0, data.length);
					long currentTime = System.nanoTime();

					avgSpeed = (avgSpeed * count + (pico/(currentTime - timeStart))) / (count + 1.f)  ; 

					if (readBytes <= 0) {
						downloadComplete = true;
						break;
					} else {
						bufferedOut.write(data, 0, readBytes);
						alreadyReadBytes += readBytes;
					}

					int percentage = (alreadyReadBytes * 100) / contentLength;

					System.err.printf("\r%3s%%[", contentLength > 0 ? percentage : "?");

					int i = 0;

					for (; i < percentage; i++) {
						if (i != percentage - 1) {
							System.err.print("=");
						} else {
							System.err.print(">");
						}
					}

					for (; i < width; i++) {
						System.err.print(" ");
					}

					System.err.print("]");

					System.err.printf("\t%,d\t\t%.2fK/s", alreadyReadBytes, avgSpeed);

					count++;
				}
			} finally {
				try {
					bufferedOut.close();
					fileOutput.close();
				} catch (IOException e) {
				}
			}
		} catch (IOException e) {
			ok = false;
		}

		System.out.printf("\n\n%s (%.2f KB/s) - `%s` saved [%,d/%,d]\n", sdf.format(new Date()), avgSpeed, fileName, alreadyReadBytes, contentLength > 0 ? contentLength : alreadyReadBytes);

		connection.disconnect();

	}

	private String getUrlDomainName(String url) {
		String domainName = new String(url);

		int index = domainName.indexOf("://");

		if (index != -1) {
			// keep everything after the "://"
			domainName = domainName.substring(index + 3);
		}

		index = domainName.indexOf('/');

		if (index != -1) {
			// keep everything before the '/'
			domainName = domainName.substring(0, index);
		}
		return domainName;
	}

	public static void main(String[] args) {
		new SimpleWebGet("http://www.asu.edu/index.html");
		new SimpleWebGet("http://kset.kz/index.php");
		new SimpleWebGet("http://kset.kz/indexx.php");
		new SimpleWebGet("http://kkset.kz/index.php");
	}
}
