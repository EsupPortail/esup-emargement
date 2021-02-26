package org.esupportail.emargement.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Component
public class ToolUtil {
	
	public int compareDate(Date date1, Date date2, String pattern) {
		DateFormat dateFormat = new SimpleDateFormat(pattern);  
        String strDate1= dateFormat.format(date1);
        String strDate2 =  dateFormat.format(date2);
		
        return strDate1.compareTo(strDate2);
	}
	
	public Pageable updatePageable(final Pageable source, final int size)
	{
	    return PageRequest.of(source.getPageNumber(), size, source.getSort());
	}
	
	public static InputStream generateQRCodeImage(String text, int width, int height)
			throws WriterException, IOException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
        byte[] bytes = bos.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        bos.close();
        return inputStream;
	}
}
