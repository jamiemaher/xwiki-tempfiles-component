/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.linwoodhomes.internal;

import java.io.File;
import java.io.IOException;

import com.linwoodhomes.TempFile;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileCleaningTracker;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of a <tt>TempFile</tt> component.
 */
@Component
@Singleton
public class DefaultTempFile implements TempFile
{
	@Inject
    private Logger logger;

	/** The default threshold in bytes before the file is committed to disk instead of memory
	 */
	protected static final int defaultFileSizeThreshold = 10000;
	
	protected int numTempFilesCount = 0; 
	
	DiskFileItemFactory fileFactory = null;
	/** Use a persistant cleaning tracker */
	FileCleaningTracker cleaningTracker = new FileCleaningTracker();
	
	public DefaultTempFile() {        	
		resetFileFactory();
    }
	
	/** Create a new file factory so that the previous instance
	 * will be garbage collected and it's temporary files deleted.
	 * 
	 * Lets try to use the temp cleaning tracker ourselves.
	 */
	protected void resetFileFactory(){
		String tempFileLocation = System.getProperty("java.io.tmpdir");
		fileFactory = new DiskFileItemFactory(defaultFileSizeThreshold,new File(tempFileLocation));
    	fileFactory.setFileCleaningTracker(null); //let us handle the tracking.
	}
	
	@Override
	public FileItem getTempFile(){
		
//		if(++numTempFilesCount > 30){
//			resetFileFactory();
//		}
		
		DiskFileItem tempFile = (DiskFileItem) fileFactory.createItem(null, null, false, null);
		
		try{
			tempFile.getOutputStream();
			
			//track the temp file.
			File storeLocation = tempFile.getStoreLocation();
			if(storeLocation != null){
				cleaningTracker.track(storeLocation,tempFile);
			}else{
				logger.debug("storeLocation was null, not tracking temp file: "+tempFile);
			}
		}catch(IOException ioe){
			logger.error("Could not create temp file, do you have write access to: "+fileFactory.getRepository());
		}
		
	    return tempFile;
	}
	
	/** Retrieve the number of temporary files by getting the file cleaning
	 * tracker's track count.
	 */
	@Override
	public int getTempFileSize() {	    
	    return this.cleaningTracker.getTrackCount();
	}
}

