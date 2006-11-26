/*************************************************************************
 *
 * $RCSfile: RegmergeBuilder.java,v $
 *
 * $Revision: 1.5 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2006/11/26 21:33:41 $
 *
 * The Contents of this file are made available subject to the terms of
 * either of the GNU Lesser General Public License Version 2.1
 *
 * Sun Microsystems Inc., October, 2000
 *
 *
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * Copyright 2000 by Sun Microsystems, Inc.
 * 901 San Antonio Road, Palo Alto, CA 94303, USA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 * 
 * The Initial Developer of the Original Code is: Sun Microsystems, Inc..
 *
 * Copyright: 2002 by Sun Microsystems, Inc.
 *
 * All Rights Reserved.
 *
 * Contributor(s): Cedric Bosdonnat
 *
 *
 ************************************************************************/
package org.openoffice.ide.eclipse.core.builders;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.openoffice.ide.eclipse.core.PluginLogger;
import org.openoffice.ide.eclipse.core.model.IUnoidlProject;
import org.openoffice.ide.eclipse.core.model.ProjectsManager;

/**
 * Builder for the URD files generating the <code>types.rdb</code> registry.
 * 
 * <p>This builder should not be associated directly
 * to a UNO project: the right builder for this is {@link TypesBuilder}. 
 * This builder doesn't make any difference between full and incremental 
 * builds.</p>
 * 
 * @author cbosdonnat
 *
 */
public class RegmergeBuilder {
		
	/**
	 * Root of the generated types, used by regmerge and javamaker. UCR 
	 * is chosen for OpenOffice.org compatibility 
	 */
	public static final String TYPE_ROOT_KEY = "/UCR"; //$NON-NLS-1$
	
	/**
	 * Computes the full build of all the <code>urd</code> files into a single
	 * <code>types.rdb</code> file. This resulting file is given by 
	 * {@link IUnoidlProject#getTypesPath()}. This methods simply launches the
	 * {@link RegmergeBuildVisitor} on the urd folder.
	 * 
	 * @param project the project to build
	 * @param monitor a monitor to watch the build progress
	 * @throws CoreException is thrown is anything wrong happens
	 */
	public static void build(IUnoidlProject project, IProgressMonitor monitor)
			throws CoreException {
		
		try {
			// The registry file is placed in the root of the project as announced 
			// to the api-dev mailing-list
			IFolder urdFolder = project.getFolder(
					project.getUrdPath());
			
			
			IFile mergeFile = project.getFile(project.getTypesPath());
			if (mergeFile.exists()){
				mergeFile.delete(true, monitor);
			}
			
			// compile each idl file
			urdFolder.accept(new RegmergeBuildVisitor(monitor));
			
		} catch (CoreException e) {
			PluginLogger.error(
					Messages.getString("RegmergeBuilder.RegmergeError"), e); //$NON-NLS-1$
		}
	}

	/**
	 * Convenience method to execute the <code>regmerge</code> tool 
	 * on a given file.
	 * 
	 * @param file the file to run <code>regmerge</code> on.
	 * @param monitor a progress monitor
	 */
	static void runRegmergeOnFile(IFile file, IProgressMonitor monitor){
		
		IUnoidlProject project = ProjectsManager.getInstance().getProject(
				file.getProject().getName());
		
		// The registry file is placed in the root of the project as announced 
		// to the api-dev mailing-list
		IFile mergeFile = project.getFile(project.getTypesPath());
		
		String existingReg = ""; //$NON-NLS-1$
		if (mergeFile.exists()){
			existingReg = mergeFile.getProjectRelativePath().toOSString() + " "; //$NON-NLS-1$
		}
		
		String command = "regmerge types.rdb " + TYPE_ROOT_KEY + " " + //$NON-NLS-1$ //$NON-NLS-2$
						   existingReg + file.getProjectRelativePath().toOSString();
		
		// Process creation
		Process process = project.getSdk().runTool(project, command, monitor);
		
		// Just wait for the process to end before destroying it
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			// Process has been interrupted by the user
		}
	}
}
