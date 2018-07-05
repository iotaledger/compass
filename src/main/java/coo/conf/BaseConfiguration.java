package coo.conf;

import com.beust.jcommander.Parameter;

public class BaseConfiguration {
    @Parameter(names = "-layers", description = "Path to folder containing Merkle Tree layers", required = true)
    public String layersPath;

    @Parameter(names = "-host", description = "URL for IRI host", required = true)
    public String host;

    @Parameter(names = "-mwm", description = "Minimum Weight Magnitude", required = true)
    public int MWM = 9;

    @Parameter(names = "-seed", description = "Seed", required = true)
    public String seed;

    @Parameter(names = "-broadcast", description = "Should Coordinator really broadcast milestones?")
    public boolean broadcast = false;

    @Parameter(names = "-mode", description = "Hashing mode (one of CURLP27, CURLP81, KERL)", required = true)
    public String mode = "CURLP27";
}
