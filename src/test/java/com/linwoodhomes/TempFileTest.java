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
package com.linwoodhomes;

import java.io.IOException;

import org.apache.commons.fileupload.FileItem;
import org.junit.Assert;
import org.junit.Test;
import com.linwoodhomes.internal.DefaultTempFile;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Tests for the {@link TempFile} component.
 */
public class TempFileTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private DefaultTempFile tf;

    @Test
    public void testGetTempFile()
    {
    	FileItem fileItem = tf.getTempFile();    	
    	try{
    		Assert.assertNotNull(fileItem.getOutputStream());
    	}catch (IOException e) {
			Assert.fail("Couldn't open output stream");
		}
    }
}