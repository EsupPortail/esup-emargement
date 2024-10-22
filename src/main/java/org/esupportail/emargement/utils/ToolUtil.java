package org.esupportail.emargement.utils;

import static com.cronutils.model.CronType.QUARTZ;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Component
public class ToolUtil {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public int compareDate(Date date1, Date date2, String pattern) {
		int i = -1;
		DateFormat dateFormat = new SimpleDateFormat(pattern);  
        String strDate1 = dateFormat.format(date1);
        String strDate2 =  dateFormat.format(date2);

        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
			Date date10 = sdf.parse(strDate1);
			Date date20 = sdf.parse(strDate2);
			i = date10.compareTo(date20);
		} catch (ParseException e) {
			log.error("Erreur lors du trairement de la date ", e);
		}

        return i;
	}
	
	public Pageable updatePageable(final Pageable source, final int size)
	{
	    return PageRequest.of(source.getPageNumber(), size, source.getSort());
	}
	
	public InputStream generateQRCodeImage(String text, int width, int height)
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
	
	public boolean isLong(String strNum) {
	    if (strNum == null) {
	        return false;
	    }
	    try {
	        Long.parseLong(strNum);
	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	
	public String getDureeEpreuve(Date heureEpreuve, Date finEpreuve, Long duration) {
		String duree ="";
		long diff = 0;
		if (duration == null) {
			diff = finEpreuve.getTime() - heureEpreuve.getTime();
		}else {
			diff = Long.valueOf(duration)*60*1000;
		}
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		if(diffHours != 0) {
			duree = String.valueOf(diffHours).concat("H");
		}
		if(diffMinutes != 0) {
			duree = duree.concat(StringUtils.leftPad(String.valueOf(diffMinutes), 2, "0"));
			if(diffHours == 0) {
				duree = duree.concat("mn");
			}
		}
		
		return duree;
	}
	
	public static String encodeToBase64(String message) {
	  return Base64.getEncoder().encodeToString(message.getBytes());
	}

	public static String decodeFromBase64(String encodedMessage) {
	  byte[] decodedBytes = Base64.getDecoder().decode(encodedMessage);
	  String decodedString = new String(decodedBytes);
	  return decodedString;
	}
	
	public String getBase64ImgFromInputStream(InputStream inputStream) {
		String base64Image = "";
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			byte[] imageBytes = outputStream.toByteArray();
			Base64.Encoder encoder = Base64.getEncoder();
			base64Image = encoder.encodeToString(imageBytes);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return base64Image; 
	}
	
    private static void resetTime(Calendar calendar, boolean resetToStartOfDay) {
        if (resetToStartOfDay) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
        }
    }
    
    public String getCronExpression(String cronExp) {
    	if("-".equals(cronExp.trim())) {
    		return "Pas d'import de configuré (Cron)";
    	}
    	
		CronDefinition cronDefinition =
		CronDefinitionBuilder.instanceDefinitionFor(QUARTZ);
		CronParser parser = new CronParser(cronDefinition);
		CronDescriptor descriptor = CronDescriptor.instance(Locale.FRANCE);
    	return descriptor.describe(parser.parse(cronExp));
    }
    
    public String convertirSecondes(Integer secondes) {
    	if(secondes == null) {
    		return " - ";
    	}
        int heures = secondes / 3600;
        int minutes = (secondes % 3600) / 60;
        int resteSecondes = secondes % 60;
        String duree = "";
        duree += (heures>0)? String.valueOf(heures) + " heure(s) " : "";
        duree += (minutes>0)? String.valueOf(minutes) + " minute(s) " : "";
        duree += (resteSecondes>0)? String.valueOf(resteSecondes) + " seconde(s) " : "";
        return duree;
    }
}
