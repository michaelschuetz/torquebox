package org.torquebox.messaging.deployers;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.Set;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jruby.Ruby;
import org.junit.Before;
import org.junit.Test;
import org.torquebox.messaging.metadata.MessageProcessorMetaData;
import org.torquebox.test.mc.vdf.AbstractDeployerTestCase;
import org.torquebox.test.ruby.TestRubyFactory;

public class MessagingTqParsingDeployerTest extends AbstractDeployerTestCase {
	
	private MessagingTqParsingDeployer deployer;
	private Ruby ruby;

	@Before
	public void setUpDeployer() throws Throwable {
		this.deployer = new MessagingTqParsingDeployer();
		addDeployer( this.deployer );
		this.ruby = TestRubyFactory.createRuby();
		this.ruby.evalScriptlet( "require 'vfs'" );
	}
	
	@Test
	public void testEmptyMessagingTq() throws Exception {
		URL messagingTq = getClass().getResource( "empty-messaging.rb" );
		String deploymentName = addDeployment( messagingTq, "messaging.tq" );
		
		DeploymentUnit unit = getDeploymentUnit( deploymentName );
		unit.addAttachment( Ruby.class, this.ruby );
		
		processDeployments( true );
		
		Set<? extends MessageProcessorMetaData> allMetaData = unit.getAllMetaData( MessageProcessorMetaData.class );
		
		assertTrue( allMetaData.isEmpty() );
		undeploy( deploymentName );
	}
	
	@Test(expected=DeploymentException.class)
	public void testJunkMessagingTq() throws Exception {
		URL messagingTq = getClass().getResource( "junk-messaging.rb" );
		String deploymentName = addDeployment( messagingTq, "messaging.tq" );
		
		DeploymentUnit unit = getDeploymentUnit( deploymentName );
		unit.addAttachment( Ruby.class, this.ruby );
		
		processDeployments( true );
	}
	
	@Test
	public void testSingleMessagingTq() throws Exception {
		URL messagingTq = getClass().getResource( "single-messaging.rb" );
		String deploymentName = addDeployment( messagingTq, "messaging.tq" );
		
		DeploymentUnit unit = getDeploymentUnit( deploymentName );
		unit.addAttachment( Ruby.class, this.ruby );
		
		processDeployments( true );
		
		Set<? extends MessageProcessorMetaData> allMetaData = unit.getAllMetaData( MessageProcessorMetaData.class );
		
		assertEquals( 1, allMetaData.size() );
		
		MessageProcessorMetaData metaData = allMetaData.iterator().next();
		
		assertNotNull( metaData );
		
		assertEquals( "MyClass", metaData.getRubyClassName() );
		assertEquals( "/topics/foo", metaData.getDestinationName() );
		assertEquals( "myfilter", metaData.getMessageSelector() );
		byte[] rubyConfig = metaData.getRubyConfig();
		assertNotNull( rubyConfig );
		
		undeploy( deploymentName );
	}
	

}
