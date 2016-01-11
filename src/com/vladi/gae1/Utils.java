package com.vladi.gae1;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.imageio.ImageIO;


public class Utils {

  public static String readTextFile(String file) throws IOException {
    FileInputStream in = null;
    try {
      return readTextInputStream(in = new FileInputStream(file));
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }
  
  public static String readTextInputStream(InputStream in) throws IOException {
    StringWriter strWr = new StringWriter();
    byte[] buf = new byte[10000];
    int len;
    while ((len = in.read(buf)) > 0) {
      strWr.write(new String(buf, 0, len));
    }
    return strWr.toString();
  }
  
  public static byte[] readBinaryFile(String file) throws Exception {
    FileInputStream in = null;
    try {
      return readBinaryInputStream(in = new FileInputStream(file));
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  public static byte[] readBinaryInputStream(InputStream inp) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[10000];
    int len;
    while ((len=inp.read(buf)) > -1) {
      bos.write(buf, 0, len);
    }
    return bos.toByteArray();
  }

}
