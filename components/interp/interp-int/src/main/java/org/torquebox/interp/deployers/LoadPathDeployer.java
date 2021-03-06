package org.torquebox.interp.deployers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Collections;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.torquebox.interp.metadata.RubyLoadPathMetaData;
import org.torquebox.interp.metadata.RubyRuntimeMetaData;


public class LoadPathDeployer extends AbstractDeployer {

    private List<String> loadPaths = Collections.EMPTY_LIST;

    public LoadPathDeployer() {
        setStage(DeploymentStages.DESCRIBE);
        setInput(RubyRuntimeMetaData.class);
        addOutput(RubyRuntimeMetaData.class);
    }

    public void setLoadPaths(List<String> loadPaths) {
        this.loadPaths = loadPaths;
    }
    
    public List<String> getLoadPaths() {
        return this.loadPaths;
    }

    @Override
    public void deploy(DeploymentUnit unit) throws DeploymentException {
        if (unit instanceof VFSDeploymentUnit) {
            deploy((VFSDeploymentUnit) unit);
        }
    }

    public void deploy(VFSDeploymentUnit unit) throws DeploymentException {
        RubyRuntimeMetaData runtimeMetaData = unit.getAttachment(RubyRuntimeMetaData.class);
        for (String path : getLoadPaths()) {
            try {
                URL url = unit.getRoot().getChild(path).toURL();
                log.info("Adding to load path: " + url );
                RubyLoadPathMetaData loadPathMeta = new RubyLoadPathMetaData(url);
                runtimeMetaData.appendLoadPath(loadPathMeta);
            } catch (MalformedURLException e) {
                throw new DeploymentException(e);
            }
        }
    }

}
