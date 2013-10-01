package jetbrains.buildServer.buildTriggers.vcs.vault;

import jetbrains.buildServer.plugins.PluginManager;
import jetbrains.buildServer.plugins.bean.PluginInfo;
import jetbrains.buildServer.plugins.classLoaders.TeamCityClassLoader;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by Victory.Bedrosova on 9/27/13.
 */
public class VaultApiConnector {
  private static final Logger LOG = Logger.getLogger(VaultApiConnector.class);

  @NotNull
  private final PluginManager myPluginManager;
  @NotNull
  private final PluginDescriptor myPluginDescriptor;

  public VaultApiConnector(final @NotNull PluginManager pluginManager, final @NotNull PluginDescriptor pluginDescriptor) {
    myPluginManager = pluginManager;
    myPluginDescriptor = pluginDescriptor;
  }

  @NotNull
  private static File getVaultConnectionJar(@NotNull PluginDescriptor pluginDescriptor) {
    return new File(pluginDescriptor.getPluginRoot(), "standalone/vault-connection.jar");
  }

  @Nullable
  private static File getVaultApiFolder(@NotNull PluginManager pluginManager) {
    for (PluginInfo pluginInfo : pluginManager.getDetectedPlugins()) {
      if ("VaultAPI".equals(pluginInfo.getPluginName())) {
        final File pluginRoot = pluginInfo.getPluginRoot();
        final File lib = new File(pluginRoot, "lib");
        return lib.isDirectory() ? lib : pluginRoot;
      }
    }
    return null;
  }

  public synchronized boolean detectApi() {
    try {
      getVaultApiClassLoader().loadClass("VaultClientIntegrationLib.ServerOperations");
    } catch (ClassNotFoundException e) {
      LOG.debug(e.getMessage(), e);
      return false;
    } catch (NoClassDefFoundError e) {
      LOG.debug(e.getMessage(), e);
      return false;
    }
    return true;
  }

  @NotNull
  public synchronized ClassLoader getVaultApiClassLoader() {
    return new VaultApiClassLoader(getClass().getClassLoader(), getVaultConnectionJar(myPluginDescriptor), getVaultApiFolder(myPluginManager));
  }

  private static final class VaultApiClassLoader extends TeamCityClassLoader {
    private VaultApiClassLoader(@NotNull final ClassLoader parent, @NotNull File vaultConnectionJar, @Nullable File vaultApiFolder) {
      super(parent, false);

      addJar(vaultConnectionJar);
      addJars(vaultApiFolder);
    }

    private void addJars(@Nullable final File vaultApiFolder) {
      if (vaultApiFolder == null) return;

      final File[] jars = vaultApiFolder.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return vaultApiFolder.equals(dir) && name.endsWith(".jar") && !isLog4j(name);
        }
      });

      if (jars == null) return;

      for (File jar : jars) {
        addJar(jar);
      }
    }

    private boolean isLog4j(@NotNull String fileName) {
      return fileName.matches("log4j\\-.*\\.jar");
    }
  }
}
