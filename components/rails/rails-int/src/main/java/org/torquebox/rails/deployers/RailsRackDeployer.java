/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.torquebox.rails.deployers;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.deployer.AbstractSimpleVFSRealDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.torquebox.mc.AttachmentUtils;
import org.torquebox.rack.metadata.RackApplicationMetaData;
import org.torquebox.rack.spi.RackApplicationPool;
import org.torquebox.rails.metadata.RailsApplicationMetaData;

public class RailsRackDeployer extends AbstractSimpleVFSRealDeployer<RailsApplicationMetaData> {

	//private static final Logger log = Logger.getLogger(RailsRackDeployer.class);
	
	public RailsRackDeployer() {
		super(RailsApplicationMetaData.class);
		addInput(RackApplicationMetaData.class);
		addOutput(RackApplicationMetaData.class);
		setStage(DeploymentStages.POST_PARSE);
	}
	
	@Override
	public void deploy(VFSDeploymentUnit unit, RailsApplicationMetaData railsAppMetaData) throws DeploymentException {

		log.info( "deploy(" + unit + ")" );
		RackApplicationMetaData rackMetaData = unit.getAttachment(RackApplicationMetaData.class);

		if (rackMetaData == null) {
			rackMetaData = new RackApplicationMetaData();
			rackMetaData.setContextPath("/");
			unit.addAttachment(RackApplicationMetaData.class, rackMetaData);
		}

		//String appPoolName = RubyRackApplicationPoolDeployer.getBeanName( unit );
		//String appPoolName = AttachmentUtils.beanName(unit, RackApplicationPool.class );
		//rackMetaData.setRackApplicationPoolName(appPoolName);
		rackMetaData.setStaticPathPrefix( "/public" );
		//rackMetaData.setRackApplicationPoolName( AttachmentUtils.beanName(unit, RackApplicationPool.class ) );
		

		rackMetaData.setRackRoot( railsAppMetaData.getRailsRoot() );
		rackMetaData.setRackEnv( railsAppMetaData.getRailsEnv() );
		
		String rackUpScript = getRackUpScript( rackMetaData.getContextPath() );
		rackMetaData.setRackUpScript( rackUpScript );

		unit.addAttachment(RackApplicationMetaData.class, rackMetaData);
	}
	
	protected String getRackUpScript(String context) {
		if ( context.endsWith( "/" ) ) {
			context = context.substring( 0, context.length() - 1 );
		}
		return "TORQUEBOX_RACKUP_CONTEXT=%q(" + context + ")\n" +
		       "require %q(org/torquebox/rails/deployers/rackup)\n" +
		       "run TorqueBox::Rails.app\n";
			
	}

}
