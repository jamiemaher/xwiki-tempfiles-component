package com.linwoodhomes.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import com.linwoodhomes.TempFile;

@Component
@Named("tempfile")
@Singleton
public class TempFileScriptService implements ScriptService {
	
	@Inject
	private TempFile tempFile;

	/**
	 * Return the number of temporary files awaiting deletion.
	 * @return int number of temporary files created, but not deleted.
	 */
	public int getTempFileSize(){
		return tempFile.getTempFileSize();
	}
}
