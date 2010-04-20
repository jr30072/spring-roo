package org.springframework.roo.project;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Provides common project operations. Should be subclassed by a project-specific operations subclass. 
 * 
 * @author Ben Alex
 * @author Adrian Colyer
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component(componentAbstract=true)
public abstract class AbstractProjectOperations implements ProjectOperations {
	@Reference protected MetadataService metadataService;
	@Reference protected ProjectMetadataProvider projectMetadataProvider;

	private Set<DependencyListener> listeners = new HashSet<DependencyListener>();
	private Set<RepositoryListener> repositoryListeners = new HashSet<RepositoryListener>();
	private Set<RepositoryListener> pluginRepositoryListeners = new HashSet<RepositoryListener>();
	private Set<PluginListener> pluginListeners = new HashSet<PluginListener>();
	private Set<PropertyListener> propertyListeners = new HashSet<PropertyListener>();

	public final boolean isDependencyModificationAllowed() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}

	public final boolean isPerformCommandAllowed() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null;
	}
	
	public void addDependencyListener(DependencyListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeDependencyListener(DependencyListener listener) {
		this.listeners.remove(listener);
	}
	
	private void sendDependencyAdditionNotifications(Dependency dependency) {
		for (DependencyListener listener : listeners) {
			listener.dependencyAdded(dependency);
		}
	}
	
	private void sendDependencyRemovalNotifications(Dependency dependency) {
		for (DependencyListener listener :listeners) {
			listener.dependencyRemoved(dependency);
		}
	}
	
	public void addRepositoryListener(RepositoryListener listener) {
		this.repositoryListeners.add(listener);
	}
	
	public void removeRepositoryListener(RepositoryListener listener) {
		this.repositoryListeners.remove(listener);
	}
	
	private void sendRepositoryAdditionNotifications(Repository repository) {
		for (RepositoryListener listener : repositoryListeners) {
			listener.repositoryAdded(repository);
		}
	}
	
	private void sendRepositoryRemovalNotifications(Repository repository) {
		for (RepositoryListener listener : repositoryListeners) {
			listener.repositoryRemoved(repository);
		}
	}
	
	public void addPluginRepositoryListener(RepositoryListener listener) {
		this.pluginRepositoryListeners.add(listener);
	}
	
	public void removePluginRepositoryListener(RepositoryListener listener) {
		this.pluginRepositoryListeners.remove(listener);
	}
	
	private void sendPluginRepositoryAdditionNotifications(Repository repository) {
		for (RepositoryListener listener : pluginRepositoryListeners) {
			listener.repositoryAdded(repository);
		}
	}
	
	private void sendPluginRepositoryRemovalNotifications(Repository repository) {
		for (RepositoryListener listener : pluginRepositoryListeners) {
			listener.repositoryRemoved(repository);
		}
	}
	
	public void addPluginListener(PluginListener listener) {
		this.pluginListeners.add(listener);
	}
	
	public void removePluginListener(PluginListener listener) {
		this.pluginListeners.remove(listener);
	}
	
	private void sendPluginAdditionNotifications(Plugin plugin) {
		for (PluginListener listener : pluginListeners) {
			listener.pluginAdded(plugin);
		}
	}
	
	private void sendPluginRemovalNotifications(Plugin plugin) {
		for (PluginListener listener : pluginListeners) {
			listener.pluginRemoved(plugin);
		}
	}

	public void addPropertyListener(PropertyListener listener) {
		this.propertyListeners.add(listener);
	}
	
	public void removePropertyListener(PropertyListener listener) {
		this.propertyListeners.remove(listener);
	}
	
	private void sendPropertyAdditionNotifications(Property property) {
		for (PropertyListener listener : propertyListeners) {
			listener.propertyAdded(property);
		}
	}
	
	private void sendPropertyRemovalNotifications(Property property) {
		for (PropertyListener listener : propertyListeners) {
			listener.propertyRemoved(property);
		}
	}

	public void updateProjectType(ProjectType projectType) {
		Assert.notNull(projectType, "ProjectType required");
		projectMetadataProvider.updateProjectType(projectType);
	}
	
	public final void addDependency(Dependency dependency) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);		
	}
	
	public final void addDependency(JavaPackage groupId, JavaSymbolName artifactId, String version) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);
	}
	
	public final void removeDependency(Dependency dependency) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		projectMetadataProvider.removeDependency(dependency);
		sendDependencyAdditionNotifications(dependency);		
	}

	public final void removeDependency(JavaPackage groupId, JavaSymbolName artifactId, String version) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.hasText(version, "Version required");
		Dependency dependency = new Dependency(groupId, artifactId, version, DependencyType.JAR, DependencyScope.COMPILE);
		projectMetadataProvider.removeDependency(dependency);
		sendDependencyRemovalNotifications(dependency);
	}
	
	public void dependencyUpdate(Dependency dependency) {
		Assert.isTrue(isDependencyModificationAllowed(), "Dependency modification prohibited at this time");
		Assert.notNull(dependency, "Dependency required");
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
		
		if (projectMetadata.isDependencyRegistered(dependency)) {
			// Already exists, so just quit
			return;
		}
		
		// Delete any existing dependencies with a different version
		for (Dependency existing : projectMetadata.getDependenciesExcludingVersion(dependency)) {
			projectMetadataProvider.removeDependency(existing);
			sendDependencyRemovalNotifications(existing);
		}
		
		// Add the dependency
		projectMetadataProvider.addDependency(dependency);
		sendDependencyAdditionNotifications(dependency);
	}

	public final void addRepository(Repository repository) {
		Assert.isTrue(isDependencyModificationAllowed(), "Repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.addRepository(repository);
		sendRepositoryAdditionNotifications(repository);
	}

	public final void removeRepository(Repository repository) {
		Assert.isTrue(isDependencyModificationAllowed(), "Repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.removeRepository(repository);
		sendRepositoryRemovalNotifications(repository);
	}

	public final void addPluginRepository(Repository repository) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.addPluginRepository(repository);
		sendPluginRepositoryAdditionNotifications(repository);
	}

	public final void removePluginRepository(Repository repository) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin repository modification prohibited at this time");
		Assert.notNull(repository, "Repository required");
		projectMetadataProvider.removePluginRepository(repository);
		sendPluginRepositoryRemovalNotifications(repository);
	}

	public final void addBuildPlugin(Plugin plugin) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.addBuildPlugin(plugin);
		sendPluginAdditionNotifications(plugin);
	}
	
	public final void removeBuildPlugin(Plugin plugin) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		projectMetadataProvider.removeBuildPlugin(plugin);
		sendPluginRemovalNotifications(plugin);
	}
	
	public void buildPluginUpdate(Plugin plugin) {
		Assert.isTrue(isDependencyModificationAllowed(), "Plugin modification prohibited at this time");
		Assert.notNull(plugin, "Plugin required");
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata unavailable");
		
		if (projectMetadata.isBuildPluginRegistered(plugin)) {
			// Already exists, so just quit
			return;
		}
		
		// Delete any existing plugin with a different version
		for (Plugin existing : projectMetadata.getBuildPluginsExcludingVersion(plugin)) {
			projectMetadataProvider.removeBuildPlugin(existing);
			sendPluginRemovalNotifications(existing);
		}
		
		// Add the plugin
		projectMetadataProvider.addBuildPlugin(plugin);
		sendPluginAdditionNotifications(plugin);
	}
	
	public final void addProperty(Property property) {
		Assert.isTrue(isDependencyModificationAllowed(), "Property modification prohibited at this time");
		Assert.notNull(property, "Property required");		
		projectMetadataProvider.addProperty(property);
		sendPropertyAdditionNotifications(property);
	}
	
	public final void removeProperty(Property property) {
		Assert.isTrue(isDependencyModificationAllowed(), "Property modification prohibited at this time");
		Assert.notNull(property, "Property required");		
		projectMetadataProvider.removeProperty(property);
		sendPropertyRemovalNotifications(property);
	}
}
