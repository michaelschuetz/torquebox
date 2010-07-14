package org.torquebox.messaging.deployers;

import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.beans.metadata.plugins.builder.BeanMetaDataBuilderFactory;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.ValueMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.torquebox.interp.spi.RubyRuntimePool;
import org.torquebox.mc.AttachmentUtils;
import org.torquebox.mc.JndiRefMetaData;
import org.torquebox.messaging.core.AbstractManagedDestination;
import org.torquebox.messaging.core.ManagedQueue;
import org.torquebox.messaging.core.ManagedTopic;
import org.torquebox.messaging.core.RubyMessageProcessor;
import org.torquebox.messaging.metadata.AbstractDestinationMetaData;
import org.torquebox.messaging.metadata.MessageProcessorMetaData;
import org.torquebox.messaging.metadata.QueueMetaData;

public class MessageProcessorDeployer extends AbstractDeployer {

	public MessageProcessorDeployer() {
		setStage(DeploymentStages.REAL);
		addInput(MessageProcessorMetaData.class);
		addOutput(BeanMetaData.class);
		setRelativeOrder(1000);
	}

	@Override
	public void deploy(DeploymentUnit unit) throws DeploymentException {
		Set<? extends MessageProcessorMetaData> allMetaData = unit.getAllMetaData(MessageProcessorMetaData.class);

		for (MessageProcessorMetaData each : allMetaData) {
			try {
				deploy(unit, each);
			} catch (NamingException e) {
				throw new DeploymentException(e);
			}
		}
	}

	protected void deploy(DeploymentUnit unit, MessageProcessorMetaData metaData) throws NamingException {
		
		String simpleName = metaData.getDestinationName() + "." + metaData.getRubyClassName();
		String beanName = AttachmentUtils.beanName( unit, RubyMessageProcessor.class, simpleName );

		BeanMetaDataBuilder builder = BeanMetaDataBuilderFactory.createBuilder(beanName, RubyMessageProcessor.class.getName());

		ValueMetaData runtimePoolInject = builder.createInject(AttachmentUtils.beanName(unit, RubyRuntimePool.class, "messaging") );

		builder.addPropertyMetaData("rubyRuntimePool", runtimePoolInject);
		builder.addPropertyMetaData("rubyClassName", metaData.getRubyClassName());
		builder.addPropertyMetaData("rubyRequirePath", metaData.getRubyRequirePath());
		builder.addPropertyMetaData("messageSelector", metaData.getMessageSelector() );
		builder.addPropertyMetaData("rubyConfig", metaData.getRubyConfig() );

		Class<? extends AbstractManagedDestination> demandClass = demandDestination(unit, metaData.getDestinationName());
		
		if (demandClass != null ) {
			String destinationBeanName = AttachmentUtils.beanName(unit, demandClass, metaData.getDestinationName());
			builder.addDemand(destinationBeanName, ControllerState.START, ControllerState.INSTALLED, null);
		}

		Context context = new InitialContext();

		//JndiRefMetaData destinationJndiRef = new JndiRefMetaData(context, metaData.getDestinationName());
		ValueMetaData destinationJndiRef = builder.createInject("jndi:" + metaData.getDestinationName() );
		builder.addPropertyMetaData("destination", destinationJndiRef);
		
		//JndiRefMetaData connectionFactoryJndiRef = new JndiRefMetaData(context, "/ConnectionFactory");
		ValueMetaData connectionFactoryJndiRef = builder.createInject("jndi:/ConnectionFactory");
		builder.addPropertyMetaData("connectionFactory", connectionFactoryJndiRef);

		BeanMetaData beanMetaData = builder.getBeanMetaData();

		unit.addAttachment(BeanMetaData.class.getName() + "$" + beanName, beanMetaData, BeanMetaData.class);
	}

	protected Class<? extends AbstractManagedDestination> demandDestination(DeploymentUnit unit, String destinationName) {
		Set<? extends AbstractDestinationMetaData> destinations = unit.getAllMetaData( AbstractDestinationMetaData.class );

		for ( AbstractDestinationMetaData each : destinations ) {
			if ( each.getName().equals( destinationName ) ) { 
				if ( each.getClass() == QueueMetaData.class ) {
					return ManagedQueue.class;
				} else {
					return ManagedTopic.class;
				}
			}
		}
		
		return null;
	}

}