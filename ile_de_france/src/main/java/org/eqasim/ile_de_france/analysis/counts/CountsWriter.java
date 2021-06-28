package org.eqasim.ile_de_france.analysis.counts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class CountsWriter {
	private final Map<Id<Link>, List<Integer>> counts;
	private final Network network;

	public CountsWriter(Map<Id<Link>, List<Integer>> counts, Network network) {
		this.counts = counts;
		this.network = network;
	}

	public void write(File path) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

		writer.write(String.join(";", new String[] { "link_id", "hour", "count", "lanes" }) + "\n");

		for (Map.Entry<Id<Link>, List<Integer>> item : counts.entrySet()) {
			for (int hour = 0; hour < 24; hour++) {
				writer.write(String.join(";", new String[] { //
						item.getKey().toString(), //
						String.valueOf(hour), //
						String.valueOf(item.getValue().get(hour)), //
						String.valueOf(network.getLinks().get(item.getKey()).getNumberOfLanes()), //
				}) + "\n");
			}
		}

		writer.close();
	}
}
