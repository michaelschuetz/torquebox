package org.torquebox.jobs.deployers;

import java.util.Set;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.ValueMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.torquebox.interp.spi.RubyRuntimePool;
import org.torquebox.jobs.core.RubyScheduler;
import org.torquebox.jobs.metadata.ScheduledJobMetaData;
import org.torquebox.mc.AttachmentUtils;

public class RubySchedulerDeployer extends AbstractDeployer {
	
	private String runtimePoolName;

	public RubySchedulerDeployer() {
		setAllInputs( true );
		setStage( DeploymentStages.PRE_REAL );
	}
	
	public void setRubyRuntimePoolName(String runtimePoolName) {
		this.runtimePoolName = runtimePoolName;
	}
	
	public String getRubyRuntimePoolName() {
		return this.runtimePoolName;
	}

	public void deploy(DeploymentUnit unit) throws DeploymentException {
		Set<? extends ScheduledJobMetaData> allMetaData = unit.getAllMetaData( ScheduledJobMetaData.class );
		
		if ( allMetaData.isEmpty() ) {
			return;
		}
		
		String beanName = AttachmentUtils.beanName( unit, RubyScheduler.class );
		BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder( beanName, RubyScheduler.class.getName() );
		
		builder.addPropertyMetaData( "name", "RubyScheduler$" + unit.getSimpleName() );
		
		String runtimePoolBeanName = this.runtimePoolName;
		
		if ( runtimePoolBeanName == null ) {
			runtimePoolBeanName = AttachmentUtils.beanName(unit, RubyRuntimePool.class, "jobs" );
		}
		
		ValueMetaData runtimePoolInjection = builder.createInject( runtimePoolBeanName );
		builder.addPropertyMetaData( "rubyRuntimePool", runtimePoolInjection );
		
		AttachmentUtils.attach( unit, builder.getBeanMetaData() );
	}

}
