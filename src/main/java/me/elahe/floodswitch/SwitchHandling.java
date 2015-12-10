package me.elahe.floodswitch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Ethernet;

public class SwitchHandling implements IOFMessageListener {
	protected static Logger log = LoggerFactory.getLogger(SwitchHandling.class);

	private Map<MacAddress, OFPort> table = new HashMap<>();

	@Override
	public String getName() {
		return "FloodSwitch";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		if (msg.getType() == OFType.PACKET_IN) {
			OFPacketIn pkin = (OFPacketIn) msg;
			Ethernet pkt = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

			table.put(pkt.getSourceMACAddress(), pkin.getInPort());

			OFPort port = OFPort.FLOOD;
			if (table.get(pkt.getDestinationMACAddress()) != null) {
				port = table.get(pkt.getDestinationMACAddress());
			}

			OFPacketOut.Builder pkout = sw.getOFFactory().buildPacketOut();
			pkout.setBufferId(pkin.getBufferId());
			pkout.setXid(pkin.getXid());

			// set actions
			OFActionOutput.Builder actionBuilder = sw.getOFFactory().actions().buildOutput();
			actionBuilder.setPort(port);
			pkout.setActions(Collections.singletonList((OFAction) actionBuilder.build()));

			// set data if it is included in the packetin
			if (pkin.getBufferId() == OFBufferId.NO_BUFFER) {
				pkout.setData(pkin.getData());
			}

			sw.write(pkout.build());

		}

		return Command.CONTINUE;
	}

}
