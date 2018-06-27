package com.chatnetwork.app.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import com.chatnetwork.app.config.Config;
import com.chatnetwork.app.user.AppUser;
import com.chatnetwork.app.user.CAEnrollment;

public class Util {
	
	// In general, portList file is next to manageNetwrok.sh
	static final public String portList = new String("../portList");
	
	static public HFCAClient newCAClient(String url) throws MalformedURLException {
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		Properties p =new Properties();
		p.setProperty("negotiationType", "TLS");
		HFCAClient client = HFCAClient.createNewInstance(url, p);
		
		
		client.setCryptoSuite(cryptoSuite);
		return client;
	}
	
	static public Channel newChannel(String channelName, HFClient client, Config config )
						throws InvalidArgumentException, TransactionException {
			Channel channel = client.newChannel(channelName);
	//		System.out.println("GGGGGGGGGG:    "+peerUrl);
			channel.addPeer(client.newPeer(config.getOrgName(), config.getPeerUrl()));
			channel.addEventHub(client.newEventHub("eventhub", config.getEventhubUrl()));
			channel.addOrderer(client.newOrderer("orderer", config.getOrdererUrl()));
			channel.setTransactionWaitTime(18000);
			channel.initialize();
			
			return channel;
		}

	public static CAEnrollment newEnrollment(String keyFolderPath, String certFolderPath)
			throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, CryptoException {
		File pkFolder = new File(keyFolderPath);
		File[] pkFiles = pkFolder.listFiles();
		String keyFileName = pkFiles[0].getName();
		
		File certFolder = new File(certFolderPath);
		File[] certFiles = certFolder.listFiles();
		String certFileName = certFiles[0].getName();
		
		PrivateKey key = null;
		String certificate = null;
		InputStream isKey = null;
		BufferedReader brKey = null;
	
		try {
	
			isKey = new FileInputStream(keyFolderPath + File.separator + keyFileName);
			brKey = new BufferedReader(new InputStreamReader(isKey));
			StringBuilder keyBuilder = new StringBuilder();
	
			for (String line = brKey.readLine(); line != null; line = brKey.readLine()) {
				if (line.indexOf("PRIVATE") == -1) {
					keyBuilder.append(line);
				}
			}
	
			certificate = new String(Files.readAllBytes(Paths.get(certFolderPath, certFileName)));
	
			byte[] encoded = DatatypeConverter.parseBase64Binary(keyBuilder.toString());
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
			KeyFactory kf = KeyFactory.getInstance("ECDSA");
			key = kf.generatePrivate(keySpec);
		} finally {
			isKey.close();
			brKey.close();
		}
	
		CAEnrollment enrollment = new CAEnrollment(key, certificate);
		return enrollment;
	}

	static public HFClient newHFClient(AppUser user) throws CryptoException, InvalidArgumentException {
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		HFClient client = HFClient.createNewInstance();
		client.setCryptoSuite(cryptoSuite);
		client.setUserContext(user);
		return client;
	}
	
	/**
     * Generate a targz inputstream from source folder.
     *
     * @param src        Source location
     * @param pathPrefix prefix to add to the all files found.
     * @return return inputstream.
     * @throws IOException
     */
    public static InputStream newTarGzInputStream(File src, String pathPrefix) throws IOException {
        File sourceDirectory = src;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(500000);

        String sourcePath = sourceDirectory.getAbsolutePath();

        TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(bos)));
        archiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        try {
            Collection<File> childrenFiles = org.apache.commons.io.FileUtils.listFiles(sourceDirectory, null, true);

            ArchiveEntry archiveEntry;
            FileInputStream fileInputStream;
            for (File childFile : childrenFiles) {
                String childPath = childFile.getAbsolutePath();
                String relativePath = childPath.substring((sourcePath.length() + 1), childPath.length());

                if (pathPrefix != null) {
                    relativePath = Utils.combinePaths(pathPrefix, relativePath);
                }

                relativePath = FilenameUtils.separatorsToUnix(relativePath);

                archiveEntry = new TarArchiveEntry(childFile, relativePath);
                fileInputStream = new FileInputStream(childFile);
                archiveOutputStream.putArchiveEntry(archiveEntry);

                try {
                    IOUtils.copy(fileInputStream, archiveOutputStream);
                } finally {
                    IOUtils.closeQuietly(fileInputStream);
                    archiveOutputStream.closeArchiveEntry();
                }
            }
        } finally {
            IOUtils.closeQuietly(archiveOutputStream);
        }

        return new ByteArrayInputStream(bos.toByteArray());
    }
	
	static public String getPortPrefixString(String orgName) {
		String prefix = new String();
		try (BufferedReader br = new BufferedReader(new FileReader(Util.portList))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       if (line.contains(orgName)) {
		    	   String[] parts = line.split(":");
		    	   prefix = parts[0];
		    	   return prefix;
		       }
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prefix;
	}
}
