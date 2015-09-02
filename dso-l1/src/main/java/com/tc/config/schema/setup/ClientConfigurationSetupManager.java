package com.tc.config.schema.setup;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.net.core.SecurityInfo;
import java.util.Arrays;


/**
 * @author vmad
 */
public class ClientConfigurationSetupManager implements L1ConfigurationSetupManager {
  private final String[] args;
  private final String source;
  private L2ConfigForL1.L2Data[] l2Data;
  private final SecurityInfo securityInfo;

  public ClientConfigurationSetupManager(String source, String[] args, String[] hosts, int[] ports, SecurityInfo securityInfo) {
    this.source = source;
    this.args = args;
    this.securityInfo = securityInfo;
    l2Data = new L2ConfigForL1.L2Data[hosts.length];
    for(int i = 0; i < hosts.length; i++) {
      l2Data[i] = new L2ConfigForL1.L2Data(hosts[i], ports[i], securityInfo != null ? securityInfo.isSecure() : false);
    }
  }
  
  public void addServer(String host, int port) {
    l2Data = Arrays.copyOf(l2Data, l2Data.length + 1);
    l2Data[l2Data.length - 1] = new L2ConfigForL1.L2Data(host, port, securityInfo != null ? securityInfo.isSecure() : false);
  }

  @Override
  public String[] processArguments() {
    return args;
  }

  @Override
  public boolean loadedFromTrustedSource() {
    return false;
  }

  @Override
  public String rawConfigText() {
    return source;
  }

  @Override
  public String source() {
    return source;
  }

  @Override
  public CommonL1Config commonL1Config() {
    return null;
  }

  @Override
  public L2ConfigForL1 l2Config() {
    return new L2ConfigForL1() {
      @Override
      public L2Data[] l2Data() {
        return l2Data;
      }

      @Override
      public L2Data[][] getL2DataByGroup() {
        return new L2Data[][] { l2Data };
      }
    };
  }

  @Override
  public SecurityInfo getSecurityInfo() {
    return securityInfo;
  }

  @Override
  public void setupLogging() {
  }

  @Override
  public void reloadServersConfiguration() throws ConfigurationSetupException {
  }
}