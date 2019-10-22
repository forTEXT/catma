package de.catma.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;

public class HazelCastService {

	private HazelcastInstance hazelcastClient;

	public HazelcastInstance getHazelcastClient() {
		return hazelcastClient;
	}

	public void start() {
		hazelcastClient = HazelcastClient.newHazelcastClient(HazelcastConfiguration.CLIENT_CONFIG);
	}
	
	public void stop() {
		if (hazelcastClient != null) {
			hazelcastClient.shutdown();
		}
	}
	
}
