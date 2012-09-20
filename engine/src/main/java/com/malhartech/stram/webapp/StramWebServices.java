/**
 * Copyright (c) 2012-2012 Malhar, Inc.
 * All rights reserved.
 */
package com.malhartech.stram.webapp;

import com.google.inject.Inject;
import com.malhartech.stram.ModuleManager;
import com.malhartech.stram.StramAppContext;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.apache.hadoop.security.UserGroupInformation;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * The web services implementation in the stram<p>
 * <br>
 * This class would ensure the the caller is authorized and then provide access to all the dag data stored
 * in the stram<br>
 * <br>
 *
 */
@Path(StramWebServices.PATH)
public class StramWebServices
{
  private static final Logger LOG = LoggerFactory.getLogger(StramWebServices.class);
  public static final String PATH = "/ws/v1/stram";
  public static final String PATH_INFO = "info";
  public static final String PATH_OPERATORS = "operators";
  public static final String PATH_SHUTDOWN = "shutdown";
  private final StramAppContext appCtx;
  private @Context
  HttpServletResponse response;
  private @Inject
  @Nullable
  ModuleManager dagManager;

  @Inject
  public StramWebServices(final StramAppContext context)
  {
    this.appCtx = context;
  }

  Boolean hasAccess(HttpServletRequest request)
  {
    String remoteUser = request.getRemoteUser();
    UserGroupInformation callerUGI = null;
    if (remoteUser != null) {
      callerUGI = UserGroupInformation.createRemoteUser(remoteUser);
    }
    if (callerUGI != null) {
      return false;
    }
    return true;
  }

  private void init()
  {
    //clear content type
    response.setContentType(null);
  }

  void checkAccess(HttpServletRequest request)
  {
    if (!hasAccess(request)) {
      throw new WebApplicationException(Status.UNAUTHORIZED);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public AppInfo get()
  {
    return getAppInfo();
  }

  @GET
  @Path(PATH_INFO)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public AppInfo getAppInfo()
  {
    init();
    return new AppInfo(this.appCtx);
  }

  @GET
  @Path(PATH_OPERATORS)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public ModulesInfo getModules() throws Exception
  {
    init();
    LOG.info("DAGManager: {}", dagManager);
    ModulesInfo nodeList = new ModulesInfo();
    nodeList.nodes = dagManager.getNodeInfoList();
    return nodeList;
  }

  @POST // not supported by WebAppProxyServlet, can only be called directly
  @Path(PATH_SHUTDOWN)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public JSONObject shutdown()
  {
    dagManager.shutdownAllContainers();
    return new JSONObject();
  }
}
