package net.floodlightcontroller.linkdiscovery.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IInfoProvider;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.ImmutablePort;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.LinkInfo;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LinkType;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.storage.IStorageSourceListener;
import net.floodlightcontroller.topology.NodePortTuple;

public class LinkDiscoveryManagerStatic implements IOFMessageListener,
		IOFSwitchListener, IStorageSourceListener, ILinkDiscoveryService,
		IFloodlightModule, IInfoProvider {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
		
	}

	@Override
	public Map<String, Object> getInfo(String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(ILinkDiscoveryService.class);
        // l.add(ITopologyService.class);
        return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        // We are the class that implements the service
        m.put(ILinkDiscoveryService.class, this);
        return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub

	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isTunnelPort(long sw, short port) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<Link, LinkInfo> getLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LinkInfo getLinkInfo(Link link) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LinkType getLinkType(Link lt, LinkInfo info) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFPacketOut generateLLDPMessage(long sw, short port,
			boolean isStandard, boolean isReverse) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, Set<Link>> getSwitchLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addListener(ILinkDiscoveryListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<NodePortTuple> getSuppressLLDPsInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void AddToSuppressLLDPs(long sw, short port) {
		// TODO Auto-generated method stub

	}

	@Override
	public void RemoveFromSuppressLLDPs(long sw, short port) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Short> getQuarantinedPorts(long sw) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAutoPortFastFeature() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setAutoPortFastFeature(boolean autoPortFastFeature) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<NodePortTuple, Set<Link>> getPortLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addMACToIgnoreList(long mac, int ignoreBits) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rowsModified(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rowsDeleted(String tableName, Set<Object> rowKeys) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchAdded(long switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchRemoved(long switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchActivated(long switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchPortChanged(long switchId, ImmutablePort port,
			PortChangeType type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchChanged(long switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		return null;
	}

}
