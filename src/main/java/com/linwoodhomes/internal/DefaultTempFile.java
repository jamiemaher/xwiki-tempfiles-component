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
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileCleaningTracker;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;

import com.linwoodhomes.TempFile;

/**
 * Implementation of a <tt>TempFile</tt> component to provide temporary files to
 * an instance of XWiki.<BR>
 * <BR>
 * 
 * Stores temporary files under the directory: java.io.tmpdir/xwiki-tmp/ <BR>
 * <BR>
 * On startup the temp file directory is created, if it doesn't exist and
 * cleaned out otherwise.<BR>
 * <BR>
 * 
 * Temporary Files are deleted and cleaned up when the DiskFileItem that
 * represents them is garbage collected by the commons-io FileReaper thread.<BR>
 * <BR>
 * 
 * The FileReaper thread will be asked to terminate once dispose is called.
 */
@Component
@Singleton
public class DefaultTempFile implements TempFile, Disposable, Initializable,
        EventListener {
    /** Logger provided by XWiki. **/
    @Inject
    private Logger             log;

    /**
     * The default threshold in bytes before the file is committed to disk
     * instead of memory.
     */
    protected static final int DEFAULT_SIZE_THRESHOLD = 10000;

    /** The number of temporary files requested of this component. **/
    private long             numTempFilesCount      = 0;

    /** Factory responsible for creating uniquely named temporary files. **/
    private DiskFileItemFactory fileFactory            = null;
    /** Temporary file cleaning tracker. */
    private FileCleaningTracker cleaningTracker = new FileCleaningTracker();

    /** Create an instance of the Default Temp File component provider. */
    public DefaultTempFile() {
    }

    @Override
    public final void initialize() throws InitializationException {
        resetFileFactory();
    }

    @Override
    public final void dispose() throws ComponentLifecycleException {
        cleanupResources();
    }

    /** Clean up resources by asking the cleaning tracker to exit when it's 
     * finished cleaning up any temporary files still outstanding.
     */
    private void cleanupResources() {
        log.info("DefaultTempFile: cleaning up resources");
        cleaningTracker.exitWhenFinished();
        
        log.info("DefaultTempFile: Number of temporary files requested: "
        		+ numTempFilesCount);
    }

    /**
     * Create a new file factory so that the previous instance will be garbage
     * collected and it's temporary files deleted.
     * 
     * Lets try to use the temp cleaning tracker ourselves.
     * @throws InitializationException if the file factory could not init.
     */
    private void resetFileFactory() throws InitializationException {
        File xwikiTmpDir = new File(System.getProperty("java.io.tmpdir")
                + File.separatorChar + "xwiki-tmp");

        log.info("DefaultTempFile: Setting temp file location to: "
                + xwikiTmpDir);

        try {
            if (xwikiTmpDir.exists() && xwikiTmpDir.isDirectory()) {
                // clean out any existing temporary files.
                FileUtils.cleanDirectory(xwikiTmpDir);
                log.debug("DefaultTempFile: cleaned up temporary directory");
            } else {
                // create the directory
                xwikiTmpDir.mkdir();
            }

            log.info("DefaultTempFile: Files will be created in memory with"
                    + " fileSize LESS than: "
                    + DEFAULT_SIZE_THRESHOLD
                    + " bytes. Files >= will be saved to disk.");
            fileFactory = new DiskFileItemFactory(DEFAULT_SIZE_THRESHOLD,
                    xwikiTmpDir);
            fileFactory.setFileCleaningTracker(null); // let us handle the
                                                      // tracking.

        } catch (IOException ioe) {
            log.error(
                    "Failed to initialize the temp file factory in directory: "
                            + xwikiTmpDir, ioe);
            throw new InitializationException(
                    "Could not initialize temporary File Factory", ioe);
        }
    }

    @Override
    public final FileItem getTempFile() {
        // create the new temp disk file.
        DiskFileItem tempFile = (DiskFileItem) fileFactory.createItem(null,
                null, false, null);

        try {
            tempFile.getOutputStream();

            // track the temp file.
            File storeLocation = tempFile.getStoreLocation();
            if (storeLocation != null) {
                
                // track the temporary file, based on the DiskFileItem itself as
                // the marker. Oonce the marker is garbage collected it will be
                // cleaned up (deleted) by the cleaningTracker's reaper thread.
                cleaningTracker.track(storeLocation, tempFile);
                
                if (log.isTraceEnabled()) {
                    log.trace("FileCleaningTracker added file: "
                            + storeLocation + " marked by: " + tempFile);
                }
            } else {
                log.debug("storeLocation was null, not tracking temp file: "
                        + tempFile);
            }
            
            numTempFilesCount++;
            
        } catch (IOException ioe) {
            log.error("Could not create temp file, is there write access to: "
                    + fileFactory.getRepository());
        }

        return tempFile;
    }

    /**
     * Retrieve the number of temporary files by getting the file cleaning
     * tracker's track count.
     * @return int The number of temp files that are currently being tracked.
     */
    @Override
    public final int getTempFileSize() {
        return this.cleaningTracker.getTrackCount();
    }

    /**
     * EVENT LISTENER METHODS - to listen for application shutdown until
     * dispose() is called automatically by XWiki on shutdown.
     **/

    /** Return the name for this component instance.
     * @return String the name.
     */
    @Override
    public final String getName() {
        return "temporaryfilefactory-" + super.toString();
    }

    /**
     * Return the events we are interested in consuming - only the
     * ApplicationStoppedEvent.
     * @return List containing ApplicationStoppedEvent
     */
    @Override
    public final List<Event> getEvents() {
        return Arrays.<Event> asList(new ApplicationStoppedEvent());
    }

    @Override
    public final void onEvent(final Event event, 
                final Object source, final Object data) {
        if (event instanceof ApplicationStoppedEvent) {
            // request that the file reaper shutdown
            cleanupResources();
        }
    }

    /** END EVENT LISTENER METHODS **/
}
