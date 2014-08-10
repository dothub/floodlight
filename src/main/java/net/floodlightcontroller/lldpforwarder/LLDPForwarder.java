package net.floodlightcontroller.lldpforwarder;

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
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
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
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.LLDP;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class LLDPForwarder implements IOFMessageListener,
		IFloodlightModule {


	protected static final Logger log = LoggerFactory.getLogger(LLDPForwarder.class);
	
	public static final String MODULE_NAME = "LLDPforwarder";
	protected IFloodlightProviderService floodlightProvider;
	protected IThreadPoolService threadPool;
	
	protected String topologyFileName;
    protected long readFrequency; //sec
  
   
    private SingletonTask refreshTopologyThread;
    
    private Map<String, HashMap<String, Vector<String>> > topology;
    protected ReentrantReadWriteLock topologyLock;
    
    
	@Override
	public String getName() {
		
		return LLDPForwarder.MODULE_NAME;
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		
		return (type.equals(OFType.PACKET_IN) && name.equals("linkdiscovery"));
		
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {

		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		
		Collection<Class<? extends IFloodlightService>> l = 
                new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IThreadPoolService.class);
		
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		threadPool = context.getServiceImpl(IThreadPoolService.class);
		
		this.topologyLock = new ReentrantReadWriteLock();
		this.topology = new HashMap<String, HashMap<String, Vector<String> >>();
		
		this.loadParameters(context);
		
		if(this.topologyFileName != null)
			this.loadTopology();

	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		
		log.info("Receive a Packet In");
		switch (msg.getType()) {
		
			case PACKET_IN:
				return this.handlePacketIn(sw.getId(), (OFPacketIn) msg,
                                   cntx);
			default:
				break;
		}
		
		return Command.CONTINUE;
	}
	private Command handlePacketIn(			
			
			long sw, OFPacketIn pi, FloodlightContext cntx) {
		log.info("Packet In");
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		if (eth.getPayload() instanceof LLDP) {
            return handleLldpForward(eth, sw, pi.getInPort(), true, cntx);
        }
		
		return Command.CONTINUE;
	}

	private Command handleLldpForward(
			Ethernet eth, long sw, short inPort,
            boolean isStandard, FloodlightContext cntx) {
		log.info("In Handle LLDP)");
	    
		log.info("LLDP Received from switch: "+ sw + ", port: "+ inPort);
		
		// serialize and wrap in a packet out
        byte[] data = eth.serialize();
        OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
                                                         .getMessage(OFType.PACKET_OUT);
        po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        po.setInPort(OFPort.OFPP_NONE);

        // set data and data length
        po.setLengthU(OFPacketOut.MINIMUM_LENGTH + data.length);
        po.setPacketData(data);
        
        
        //retrieve adjSwitch and adjPort
        this.topologyLock.readLock().lock();
        long adjSwitch 
        	= Long.parseLong((this.topology.get(Long.toString(sw)).get(Short.toString(inPort))
        			.elementAt(0)));
        short adjPort = Short.parseShort((this.topology.get(Long.toString(sw)).get(Short.toString(inPort))
    			.elementAt(1)));
        this.topologyLock.readLock().unlock();
        
        IOFSwitch iofSwitch = floodlightProvider.getSwitches().get(adjSwitch);
        OFPhysicalPort ofpPort = iofSwitch.getPort(adjPort);
        
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(new OFActionOutput(ofpPort.getPortNumber(), (short) 0));
        
        po.setActions(actions);
        short  actionLength = 0;
        Iterator <OFAction> actionIter = actions.iterator();
        while (actionIter.hasNext()) {
            actionLength += actionIter.next().getLength();
        }
        po.setActionsLength(actionLength);
        
        // po already has the minimum length + data length set
        // simply add the actions length to this.
        po.setLengthU(po.getLengthU() + po.getActionsLength());

        // send
        try {
            iofSwitch.write(po, null);
            iofSwitch.flush();
        } catch (IOException e) {
            log.error("Failure sending LLDP forwarding");
        }
		
		return Command.STOP;
	}
	
	
	//Added Methods
	private void loadParameters(FloodlightModuleContext context) 
	{
		// read our config options
		Map<String, String> configOptions = context.getConfigParams(this);
		
        this.topologyFileName = configOptions.get("topologyFileName");
        if(this.topologyFileName == null)
        	log.error("File Name for Cut Edges is not configured");
        
        log.info("The cutedges file name is set to: " + this.topologyFileName);
        String timer = configOptions.get("readFrequency");
        if(timer == null)
        {
        	this.readFrequency = 5; //sec
        }
        else
        	this.readFrequency = Long.parseLong(timer);
        
        log.info("The cutedges read timer is set to: " + this.readFrequency);
	}
	
	private void loadTopology() {
		
		ScheduledExecutorService ses = threadPool.getScheduledExecutor();
		
		refreshTopologyThread = new SingletonTask(ses, new Runnable() {
            @Override
            public void run() {
            	log.info("Reading thread again started");
                while (true) {
                    try {
                        refreshTopology();
                    } 
                    
                    catch (Exception e) {
                    	log.error("Exception in refreshing the topology");
                    }
                    finally {
                    	//Again scheduling
                    	refreshTopologyThread.reschedule(readFrequency, TimeUnit.SECONDS);
                    }
                }
            }
		
        });
		
		refreshTopologyThread.reschedule(readFrequency, TimeUnit.SECONDS);
		
	}
	
	private void refreshTopology() throws InterruptedException {
		
		 try {
			 log.info("Refreshing topology");
	    	 String fileNameWithPath = System.getProperty("user.dir")+"/dot/"+this.topologyFileName;
	    	 BufferedReader file = new BufferedReader(new FileReader(fileNameWithPath));
	       
			 try {
				 	topologyLock.writeLock().lock();
				 	this.topology.clear();
				 
					while (file.ready()) { 
						String text = file.readLine();
						
						//skipping comments
						if(text.charAt(0) == '#')
							continue;
						
						StringTokenizer tokenizer = new StringTokenizer(text," "); 
						int counter = 0;
						String [] values = new String[4];
						while (tokenizer.hasMoreTokens()) 
						{	
							values[counter++] = tokenizer.nextToken(); 				
						}
						
						HashMap<String, Vector<String>> tempMap;
						
						Vector<String> tempVector = new Vector<String>();
					
						tempVector.add(values[2]);
						tempVector.add(values[3]);
						
						if(this.topology.containsKey(values[0]))
						{
							this.topology.get(values[0]).put(values[1], tempVector);
						}
						else
						{
							tempMap = new HashMap<String, Vector<String>>();
							tempMap.put(values[1], tempVector);
							this.topology.put(values[0], tempMap);
						}		
						
						tempVector = new Vector<String>();						
						tempVector.add(values[0]);
						tempVector.add(values[1]);
						
						//Inserting the reverse 
						if(this.topology.containsKey(values[2]))
						{
							this.topology.get(values[2]).put(values[3], tempVector);
						}
						else
						{
							tempMap = new HashMap<String, Vector<String>>();
							tempMap.put(values[3], tempVector);
							this.topology.put(values[2], tempMap);
						}				
						
						
					}
					
				} catch (IOException e) {
					
					log.error("Failed to read data from topology file", e);
			}
			 finally {
				
				 log.debug("Topology: ");
				 for (Map.Entry<String, HashMap<String, Vector<String>>> entry : this.topology.entrySet()) {
					    
					 String output = entry.getKey() + ": ";
				
					 
					 for(Map.Entry<String, Vector<String>> eachLink: entry.getValue().entrySet())
					 {
						 output += eachLink.getKey() + " ,";
						 output += eachLink.getValue().get(0) + " ,";
						 output += eachLink.getValue().get(1)+ " ;";
						 
					 }						 
					 
					 log.debug(output);
					}
								 
				
	            topologyLock.writeLock().unlock();
	        }   

	       
	       } catch (FileNotFoundException e) {
			
	    	   log.error("Failed to load topology file", e);
	       	}
	       
	}

}
