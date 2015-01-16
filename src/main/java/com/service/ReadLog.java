/**
 * This class is used to search a log file on remote servers
 * for a term and write the remote host name to a file which will
 * then be used by a shell script to restart the service.
 */

package com.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class ReadLog {

/**
 * Reference to the log4j logger
 */
  private static final Logger LOG = Logger.getLogger(ReadLog.class);

/**
 * Reference to ChannelSftp
 */
  private static ChannelSftp sftpChannel;

/**
 * Reference to session
 */
  private static Session session = null;

/**
 * Reference to Writer
 */
  private static Writer writer;

/**
 * Reference to remote user
 */
  private static final String USER = "root";

/**
 * Reference to remote password
 */
  private static final String PWD = "password";

/**
 * A file name, path and search term are passed to this class at runtime.
 * The file is searched for the term. If the term is found the remote
 * host name is written to a file.
 */
  public static void main(final String[] args) {

    FileInputStream fstream = null;
    BufferedReader reader = null;

    try {
      fstream = new FileInputStream("serverList.txt");
      reader = new BufferedReader(new InputStreamReader(fstream));
      writer = new BufferedWriter(new FileWriter("NeedToRestart.txt"));
      String host = "";

      while ((host = reader.readLine()) != null)   {
    	  
        if (LOG.isDebugEnabled()) {
          LOG.debug(host);    
        }
        connectToServer(host);
        readLog(host, args[0], args[1]);
      }
    } catch (IOException e) {
      LOG.error("Error: " + e.getMessage());

    } finally {
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(fstream);
      IOUtils.closeQuietly(writer);
    }
  }
    
/**
 * This method creates a secure connection to a remote host.
 * @param host The remote host
 */
  private static void connectToServer(final String host) {

    JSch jsch = null;    

    try {
      jsch = new JSch();
      session = jsch.getSession(USER, host, 22);
      session.setPassword(PWD);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();
    
      if (LOG.isInfoEnabled()) {
        LOG.info("Connection established to: " + host);
      }

      sftpChannel = (ChannelSftp) session.openChannel("sftp");
      sftpChannel.connect();

    } catch (JSchException e) {
      LOG.error("JSchException: " + e.getMessage());
    }
  }
    
/**
 * This method reads the log file on the remote host and searches it for
 * a term. If the term is found the remote host name is written to a text
 * file.
 * @param host The remote host
 * @param log The remote log
 * @param searchTerm The search term
 */
  private static void readLog(final String host, final String log, final String searchTerm) {

    InputStream stream = null;
    BufferedReader reader = null;

    try {
      stream = sftpChannel.get(log);
      reader = new BufferedReader(new InputStreamReader(stream));
      String line = "";

      while ((line = reader.readLine()) != null) {
    	  
        if (line.indexOf(searchTerm) != -1) {

          if (LOG.isDebugEnabled()) {
            LOG.debug(host);
            LOG.debug(line);
          }
          writer.write(host + "\n");
          break;
        }
      }
      sftpChannel.exit();
      session.disconnect();

    } catch (SftpException e) {
      LOG.error("SftpException: " + e.getMessage());

    } catch (IOException e) {
      LOG.error("IOException: " + e.getMessage());
   
    } finally {
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(stream);
    }
  }
}