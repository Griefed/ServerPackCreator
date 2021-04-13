package de.griefed.serverpackcreator.curseforgemodpack;

import java.util.Random;

public class Splines {
    /*
    Thanks go out to MAXIS and Sim City, for the ridiculous messages these games contained.
    Additional thanks to Larsz on GameFAQs for providing a list of all these messages: https://gamefaqs.gamespot.com/pc/561176-simcity-4/faqs/22135
     */
    private static final String[] reticulating = {"Adding Hidden Agendas","Adjusting Bell Curves","Aesthesizing Industrial Areas",
            "Aligning Covariance Matrices","Applying Feng Shui Shaders","Applying Theatre Soda Layer",
            "Asserting Packed Exemplars","Attempting to Lock Back-Buffer","Binding Sapling Root System","Breeding Fauna",
            "Building Data Trees","Bureacritizing Bureaucracies","Calculating Inverse Probability Matrices",
            "Calculating Llama Expectoration Trajectory","Calibrating Blue Skies","Charging Ozone Layer",
            "Coalescing Cloud Formations","Cohorting Exemplars","Collecting Meteor Particles",
            "Compounding Inert Tessellations","Compressing Fish Files","Computing Optimal Bin Packing",
            "Concatenating Sub-Contractors","Containing Existential Buffer","Debarking Ark Ramp",
            "Debunching Unionized Commercial Services","Deciding What Message to Display Next",
            "Decomposing Singular Values","Decrementing Tectonic Plates","Deleting Ferry Routes",
            "Depixelating Inner Mountain Surface Back Faces","Depositing Slush Funds",
            "Destabilizing Economic Indicators","Determining Width of Blast Fronts","Deunionizing Bulldozers",
            "Dicing Models","Diluting Livestock Nutrition Variables","Downloading Satellite Terrain Data",
            "Exposing Flash Variables to Streak System","Extracting Resources","Factoring Pay Scale",
            "Fixing Election Outcome Matrix","Flood-Filling Ground Water","Flushing Pipe Network",
            "Gathering Particle Sources","Generating Jobs","Gesticulating Mimes","Graphing Whale Migration",
            "Hiding Willio Webnet Mask","Implementing Impeachment Routine","Increasing Accuracy of RCI Simulators",
            "Increasing Magmafacation","Initializing My Sim Tracking Mechanism",
            "Initializing Rhinoceros Breeding Timetable","Initializing Robotic Click-Path AI",
            "Inserting Sublimated Messages","Integrating Curves","Integrating Illumination Form Factors",
            "Integrating Population Graphs","Iterating Cellular Automata","Lecturing Errant Subsystems",
            "Mixing Genetic Pool","Modeling Object Components","Mopping Occupant Leaks","Normalizing Power",
            "Obfuscating Quigley Matrix","Overconstraining Dirty Industry Calculations",
            "Partitioning City Grid Singularities","Perturbing Matrices","Pixalating Nude Patch",
            "Polishing Water Highlights","Populating Lot Templates","Preparing Sprites for Random Walks",
            "Prioritizing Landmarks","Projecting Law Enforcement Pastry Intake","Realigning Alternate Time Frames",
            "Reconfiguring User Mental Processes","Relaxing Splines","Removing Road Network Speed Bumps",
            "Removing Texture Gradients","Removing Vehicle Avoidance Behavior","Resolving GUID Conflict",
            "Reticulating Splines","Retracting Phong Shader","Retrieving from Back Store",
            "Reverse Engineering Image Consultant","Routing Neural Network Infanstructure",
            "Scattering Rhino Food Sources","Scrubbing Terrain","Searching for Llamas",
            "Seeding Architecture Simulation Parameters","Sequencing Particles","Setting Advisor Moods",
            "Setting Inner Deity Indicators","Setting Universal Physical Constants",
            "Sonically Enhancing Occupant-Free Timber","Speculating Stock Market Indices",
            "Splatting Transforms","Stratifying Ground Layers","Sub-Sampling Water Data","Synthesizing Gravity",
            "Synthesizing Wavelets","Time-Compressing Simulator Clock","Unable to Reveal Current Activity",
            "Weathering Buildings","Zeroing Crime Network"};

    private static int getRandomInt() {
        Random randInt = new Random();
        return randInt.nextInt(reticulating.length);
    }

    private static String setSplines() {
        return Splines.reticulating[getRandomInt()];
    }

    public static String getSplines() {
        return setSplines();
    }
}
