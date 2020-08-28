package com.univocity.shopify.utils;

import com.google.zxing.*;
import com.google.zxing.client.j2se.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.*;
import org.slf4j.*;
import org.springframework.core.io.*;
import org.springframework.security.util.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;

public class QrCode {

	private static final Logger log = LoggerFactory.getLogger(Utils.class);

	public static BufferedImage generateQRCodeImage(String content) throws WriterException {
		QRCodeWriter barcodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = barcodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);
		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}

	public static InputStreamSource generateQRCodeAttachment(String content) {
		try {
			BufferedImage image = generateQRCodeImage(content);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			baos.flush();
			byte[] imageBytes = baos.toByteArray();
			baos.close();
			return new InMemoryResource(imageBytes);
		} catch (Exception e) {
			log.error("Unable to generate QR code attachment for input string: " + content, e);
			return null;
		}
	}
}
