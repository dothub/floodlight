package net.floodlightcontroller.arpresponder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.core.util.AppIDInUseException;
import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.internal.Device;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.ForwardingBase;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.topology.ITopologyService;

public class ARPResponder extends ForwardingBase implements IFloodlightModule,
		IOFMessageListener {

	public static final String MODULE_NAME = "ARPResponder";
	protected static Logger log = LoggerFactory.getLogger(ARPResponder.class);
	// flow-mod - for use in the cookie
    public static final int ARPResponder_APP_ID = 101; 
    public long appCookie = AppCookie.makeCookie(ARPResponder_APP_ID, 0);
    protected HashMap<String, String> ipMACMap;
    protected BufferedReader ipMACFile; 
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l = 
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(IDeviceService.class);
        l.add(IRoutingService.class);
        l.add(ITopologyService.class);
        l.add(ICounterStoreService.class);
        return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		super.init();
        this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        this.deviceManager = context.getServiceImpl(IDeviceService.class);
        this.routingEngine = context.getServiceImpl(IRoutingService.class);
        this.topology = context.getServiceImpl(ITopologyService.class);
        this.counterStore = context.getServiceImpl(ICounterStoreService.class);
        this.ipMACMap = new HashMap<String, String>();
        try {
            AppCookie.registerApp(ARPResponder_APP_ID, MODULE_NAME);
        } catch (AppIDInUseException e) {
            // This is not fatal, CLI will be confused
            log.error("Failed register application ID", e);
        }
        
        System.out.println("New Here");
       try {
    	  log.debug(System.getProperty("user.dir"));
    	  this.ipMACFile = new BufferedReader(new FileReader("ipMac.txt"));
       
		 try {
			
			 
				while (this.ipMACFile.ready()) { 
					String text = this.ipMACFile.readLine();
					StringTokenizer tokenizer = new StringTokenizer(text," "); 
					int counter = 0;
					String [] values = new String[2];
					while (tokenizer.hasMoreTokens()) 
					{
						if(counter < 2)
							values[counter++] = tokenizer.nextToken(); 				
					}
					System.out.println("IP_MAC "+ values[0] + " " + values[1]);
					this.ipMACMap.put(values[0], values[1]);
					log.debug(values[0] + " " + values[1]);
					
				}
				System.out.println("IP_MAC loaded");
				Iterator<Entry<String, String>> iter = this.ipMACMap.entrySet().iterator();
			       
				while (iter.hasNext()) {
					Map.Entry<String, String> mEntry = (Map.Entry<String,String>) iter.next();
					log.debug(mEntry.getKey() + " : " + mEntry.getValue());
				}
			} catch (IOException e) {
				log.error("Failed to read data from ipMac.txt", e);
		}          

       
       } catch (FileNotFoundException e) {
		
    	   log.error("Failed to load ipMac.txt", e);
       	}
       
      

	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub
		super.startUp(); //calling the forwarding base's startup. It will include this as a listener of OFPktIN
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command processPacketInMessage(
			IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision,
			FloodlightContext cntx) {
		
		//log.debug("ARPResponder: PKT_IN");
		
		ARP arp = new ARP();
		
		Ethernet ethPacket = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		
		if (ethPacket.getEtherType() != Ethernet.TYPE_ARP )
			return Command.CONTINUE;
		//log.debug("ARPResponder: Packet Type: ARP ");
		
		if (ethPacket.getPayload() instanceof ARP) {
			//log.debug("ARPResponder: Packet Payload: ARP ");
            arp = (ARP) ethPacket.getPayload();
		} else {
			return Command.CONTINUE;
		}
		
		// Handle ARP request.
		if (arp.getOpCode() == ARP.OP_REQUEST) {
			return this.ARPRequestHandler(arp, sw.getId(), pi.getInPort());
		}
		return Command.CONTINUE;
		
	}
	

    private Command ARPRequestHandler(ARP arp, long swid, short inPort) {
		
    	//log.debug("In ARPRequestHandler");
    	
		int sourceIPAddress = IPv4.toIPv4Address(arp.getSenderProtocolAddress());		
		long sourceMACAddress = Ethernet.toLong(arp.getSenderHardwareAddress());		
		int targetIPAddress = IPv4.toIPv4Address(arp.getTargetProtocolAddress());
		long targetMACAddress = 0;
		
		
		String sourceIPAddressStr = IPv4.fromIPv4Address(IPv4.toIPv4Address(arp.getSenderProtocolAddress()));
		String targetIPAddressStr = IPv4.fromIPv4Address(IPv4.toIPv4Address(arp.getTargetProtocolAddress()));
		
		//log.debug("ARPResponder: Src IP: " + sourceIPAddressStr  + " src MAC: " + sourceMACAddress + "target ip: " + targetIPAddressStr + " target MAC: " + targetMACAddress);
		/*@SuppressWarnings("unchecked")
		IDevice destinationDevice = deviceManager.findDevice(0, null, targetIPAddress, null, null);
	
		if (destinationDevice != null) {
			log.debug("Receiving device mac");
			targetMACAddress = destinationDevice.getMACAddress();
			log.debug("ARPResponder: IP: " + targetIPAddress + " MAC: " + targetMACAddress);
		}
		deviceManager.findDevice(0, null, targetIPAddress, null, null);
		deviceManager.queryDevices(null, null, (int) targetIPAddress, null, null);
		*/
		
		
		//Hack
		//TODO: No check if MAC of IP is not present
		targetMACAddress =  Ethernet.toLong(Ethernet.toMACAddress((String)this.ipMACMap.get(targetIPAddressStr)));
		//log.debug("Parsed Mac for IP " + targetIPAddressStr + " is" + targetMACAddress);		
		
		this.sendARPReply(sourceIPAddress, sourceMACAddress, targetIPAddress, targetMACAddress, swid, inPort);
		
    	return Command.STOP;
	}

    protected void sendARPReply(int sourceIPAddress, long sourceMACAddress, int targetIPAddress, long targetMACAddress, long swid, short inPort)
    {
    	//creating ARP reply
		IPacket arpReply = new Ethernet()
			.setSourceMACAddress(Ethernet.toByteArray(targetMACAddress))
	    	.setDestinationMACAddress(Ethernet.toByteArray(sourceMACAddress))
	    	.setEtherType(Ethernet.TYPE_ARP)
	    	.setPayload(new ARP()
				.setHardwareType(ARP.HW_TYPE_ETHERNET)
				.setProtocolType(ARP.PROTO_TYPE_IP)
				.setOpCode(ARP.OP_REPLY)
				.setHardwareAddressLength((byte)6)
				.setProtocolAddressLength((byte)4)
				.setSenderHardwareAddress(Ethernet.toByteArray(targetMACAddress))
				.setSenderProtocolAddress(IPv4.toIPv4AddressBytes(targetIPAddress))
				.setTargetHardwareAddress(Ethernet.toByteArray(sourceMACAddress))
				.setTargetProtocolAddress(IPv4.toIPv4AddressBytes(sourceIPAddress))
				.setPayload(new Data(new byte[] {0x01})));
		// Send ARP reply.
		 byte[] data = arpReply.serialize();
	     OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory().getMessage(OFType.PACKET_OUT);
	     po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	     po.setInPort(OFPort.OFPP_NONE);

	     // Set actions
	     List<OFAction> actions = new ArrayList<OFAction>();
	     actions.add(new OFActionOutput(inPort, (short) 0));
	     po.setActions(actions);
	     po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

	     // Set data
	     po.setLengthU(OFPacketOut.MINIMUM_LENGTH + po.getActionsLength() + data.length);
	     po.setPacketData(data);
	        
	     // Send message
	     try {
	      	//log.debug("Sending arp reply");
	      	floodlightProvider.getSwitch(swid).write(po, null);
	       	floodlightProvider.getSwitch(swid).flush();
	       } catch (IOException e) {
	       	log.error("Failure sending ARP out port {} on switch {}", new Object[] { inPort, floodlightProvider.getSwitch(swid).getStringId() }, e);
	     }

    }
	/**
     * Returns the application name 
     */
    @Override
    public String getName() {
        return this.MODULE_NAME;
    }
    
    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return (type.equals(OFType.PACKET_IN) &&
                (name.equals("topology") ||
                 name.equals("devicemanager")));
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
    	return (type.equals(OFType.PACKET_IN) &&    (name.equals("forwarding")));
    }

}
