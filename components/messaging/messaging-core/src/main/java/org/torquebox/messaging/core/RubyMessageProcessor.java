package org.torquebox.messaging.core;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.jboss.logging.Logger;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.torquebox.common.reflect.ReflectionHelper;
import org.torquebox.interp.core.RubyComponentResolver;
import org.torquebox.interp.spi.RubyRuntimePool;

public class RubyMessageProcessor implements MessageListener {

	private static final Logger log = Logger.getLogger(RubyMessageProcessor.class);

	private String name;
	private Destination destination;
	private String messageSelector;
	private ConnectionFactory connectionFactory;
	private Session session;
	private MessageConsumer consumer;
	private RubyRuntimePool rubyRuntimePool;

	private Connection connection;

	private RubyComponentResolver componentResolver;

	private byte[] rubyConfig;

	private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

	public RubyMessageProcessor() {

	}

	public String toString() {
		return "[MessageDrivenConsumer: " + getName() + "]";
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setComponentResolver(RubyComponentResolver resolver) {
		this.componentResolver = resolver;
	}

	public RubyComponentResolver getComponentResolver() {
		return this.componentResolver;
	}
	
	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public Destination getDestination() {
		return this.destination;
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	public String getMessageSelector() {
		return this.messageSelector;
	}

	public void setRubyConfig(byte[] rubyConfig) {
		this.rubyConfig = rubyConfig;
	}

	public byte[] getRubyConfig() {
		return this.rubyConfig;
	}

	public void setAcknowledgeMode(int acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	public int getAcknowledgeMode() {
		return this.acknowledgeMode;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	public void setRubyRuntimePool(RubyRuntimePool rubyRuntimePool) {
		this.rubyRuntimePool = rubyRuntimePool;
	}

	public RubyRuntimePool getRubyRuntimePool() {
		return this.rubyRuntimePool;
	}

	public void create() throws JMSException {
		log.info("creating for " + getDestination());

		this.connection = this.connectionFactory.createConnection();

		this.session = this.connection.createSession(true, this.acknowledgeMode);

		this.consumer = session.createConsumer(getDestination(), getMessageSelector());
		this.consumer.setMessageListener(this);
	}

	public void start() throws JMSException {
		log.info("starting for " + getDestination());
		if (connection != null) {
			connection.start();
		}
	}

	public void stop() throws JMSException {
		log.info("stopping for " + getDestination());
		if (this.connection != null) {
			log.info("stopping connection for " + getDestination());
			this.connection.stop();
		}
	}

	public void destroy() throws JMSException {
		log.info("destroying for " + getDestination());
		if (this.connection != null) {
			log.info("destroying connection for " + getDestination());
			this.connection.close();
			this.connection = null;
		}
	}

	@Override
	public void onMessage(Message message) {
		Ruby ruby = null;

		try {
			ruby = getRubyRuntimePool().borrowRuntime();
			IRubyObject processor = instantiateProcessor(ruby);
			configureProcessor(processor);
			processMessage(processor, message);
		} catch (Exception e) {
			log.error("unable to dispatch", e);
			e.printStackTrace();
		} finally {
			if (ruby != null) {
				getRubyRuntimePool().returnRuntime(ruby);
			}
			try {
				this.session.commit();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}

	protected IRubyObject instantiateProcessor(Ruby ruby) throws Exception {
		return this.componentResolver.resolve(ruby);
	}

	protected void configureProcessor(IRubyObject processor) {
		Ruby ruby = processor.getRuntime();
		
		Object config = null;

		if (this.rubyConfig != null) {
			//RubyModule string = ruby.getClassFromPath("String");
			//RubyString configStr = (RubyString) JavaEmbedUtils.invokeMethod( ruby, string, "from_java_bytes", new Object[] { this.rubyConfig }, RubyString.class );
			String configStr = RubyString.bytesToString( this.rubyConfig );
			
			RubyModule marshal = ruby.getClassFromPath("Marshal");
			config = JavaEmbedUtils.invokeMethod(ruby, marshal, "load", new Object[] { configStr }, Object.class);
		}
		
		if (config == null) {
			config = RubyHash.newHash( ruby );
		}
		
		System.err.println( "SETTING CONFIG: " + config );
		
		ReflectionHelper.callIfPossible(ruby, processor, "configure", new Object[] { config });
	}

	protected void processMessage(IRubyObject processor, Message message) {
		Ruby ruby = processor.getRuntime();
		System.err.println( "before invoke >>>" );
		System.err.println( "processor=" + processor );
		JavaEmbedUtils.invokeMethod(ruby, processor, "process!", new Object[] { message }, void.class);
		System.err.println( "<<<< after invoke" );
	}
}
