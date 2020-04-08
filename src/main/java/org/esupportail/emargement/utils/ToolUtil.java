package org.esupportail.emargement.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

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

}
