/*
 * Title: Training Application for Edge Scenarios
 *
 * Written by Baris Yamansavascilar
 *
 */

package edu.boun.edgecloudsim.applications.computingSim;


import edu.boun.edgecloudsim.utils.PPO.PPOPlayer;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Train {

    public static void main(String[] args){
        Log.disable();

        //启用此应用程序的控制台ourput和文件输出
        SimLogger.enablePrintLog();

        int iterationNumber = 1;
        String configFile = "";
        String outputFolder = "";
        String edgeDevicesFile = "";
        String applicationsFile = "";
        if (args.length == 5){
            configFile = args[0];
            edgeDevicesFile = args[1];
            applicationsFile = args[2];
            outputFolder = args[3];
            iterationNumber = Integer.parseInt(args[4]);
        }
        else{
            SimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
            configFile = "scripts/config/default_config.properties";
            applicationsFile = "scripts/config/applications.xml";
            edgeDevicesFile = "scripts/config/devices-4.xml";
            outputFolder = "sim_results/ite" + iterationNumber;
        }
        //初始化deepedge配置
        SimSettings SS = SimSettings.getInstance();
        if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
            SimLogger.printLine("cannot initialize simulation settings!");
            System.exit(0);
        }
//        开启日志并清空日志文件夹
        if(SS.getFileLoggingEnabled()){
//            outputFolder = "sim_results/use/data_size/"+SS.getLoadSize()+"/" + SS.getOrchestratorPolicies()[0] ;//SS.getLearningRate()
            outputFolder = "sim_results/use/device/"+SS.getMaxNumOfMobileDev()+"/" + SS.getOrchestratorPolicies()[0] ;//SS.getLearningRate()
//            outputFolder = "sim_results/use/param/DISCOUNT_FACTOR/"+SS.getLearningRate();
//            outputFolder = "sim_results/use/device/2000/DDQN--";
            outputFolder = "sim_results/useAlltopo/"+SS.getTOPO()+"/-"+SS.getOrchestratorPolicies()[0];
            System.out.println(outputFolder);
            SimLogger.enableFileLog();
            SimUtils.cleanOutputFolder(outputFolder);
        }
        for (String str : SS.getSimulationScenarios()) System.out.println(str);
        for (String str : SS.getOrchestratorPolicies()) System.out.println(str);

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date SimulationStartDate = Calendar.getInstance().getTime();
        String now = df.format(SimulationStartDate);
        SimLogger.printLine("Simulation started at " + now);
        SimLogger.printLine("----------------------------------------------------------------------");

        int numberOfEpisodes = 1;
        DQNAgent ddqnAgent = null;
        String policy = SS.getOrchestratorPolicies()[0];

        int actionsize = SS.getActionSize(),featureSize=0;
        if(actionsize==64) featureSize = 52;
        else if(actionsize==16) featureSize = 57;
        else featureSize = 20+actionsize;
        if (policy.equals("DDQN")) {
            ddqnAgent = new DQNAgent((int) featureSize);//初始化强化学习
        }
        else if(policy.equals("PPO")){
            PPOPlayer player = new PPOPlayer();
        }

        for (int episode=0; episode < numberOfEpisodes; episode++){//迭代次数
            for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())//移动设备
            {
                for(int k=0; k<SS.getSimulationScenarios().length; k++)//场景
                {
                    for(int i=0; i<SS.getOrchestratorPolicies().length; i++)//策略
                    {
                        String simScenario = SS.getSimulationScenarios()[k];
                        String orchestratorPolicy = SS.getOrchestratorPolicies()[i];
                        Date ScenarioStartDate = Calendar.getInstance().getTime();//时间
                        now = df.format(ScenarioStartDate);
                        SimLogger.printLine("Scenario started at " + now);
                        SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - #iteration: " + iterationNumber);
                        SimLogger.printLine("Duration: " + SS.getSimulationTime()/60 + " min (warm up period: "+ SS.getWarmUpPeriod()/60 +" min) - #devices: " + j);
                        SimLogger.getInstance().simStarted(outputFolder, orchestratorPolicy + "_" + j + "DEVICES");//名字

                        try
                        {
                            // 第一步：初始化CloudSim包，它应该在创建任何实体之前被调用
                            int num_user = 2;   // number of grid users
                            Calendar calendar = Calendar.getInstance();
                            boolean trace_flag = false;  // mean trace events

                            // Initialize the CloudSim library
                            CloudSim.init(num_user, calendar, trace_flag, 0.01);

                            // Generate EdgeCloudsim Scenario Factory
                            ScenarioFactory sampleFactory = new CFNcenarioFactory(j,SS.getSimulationTime(), orchestratorPolicy, simScenario);

                            // Generate EdgeCloudSim Simulation Manager
                            SimManager manager = new SimManager(sampleFactory, j, simScenario, orchestratorPolicy);


                            // Start simulation
                            manager.startSimulation();
                        }
                        catch (Exception e)
                        {
                            SimLogger.printLine("The simulation has been terminated due to an unexpected error");
                            e.printStackTrace();
                            System.exit(0);
                        }

                        Date ScenarioEndDate = Calendar.getInstance().getTime();
                        now = df.format(ScenarioEndDate);
                        SimLogger.printLine("Scenario finished at " + now +  ". It took " + SimUtils.getTimeDifference(ScenarioStartDate,ScenarioEndDate));
                        SimLogger.printLine("----------------------------------------------------------------------");
                    }//End of orchestrators loop
                }//End of scenarios loop
            }//End of mobile devices loop
            if (policy.equals("DDQN")){
                try{
                    //ddqnAgent.saveModel(Integer.toString(episode), ddqnAgent.getReward());
                    ddqnAgent.saveModel(outputFolder,Integer.toString(episode),ddqnAgent.getReward(), ddqnAgent.getAvgQvalue());
                    ddqnAgent.resetQValue();
                }catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("\n Total reward of agent for this episode: "+ ddqnAgent.getReward());
                System.out.println("\n Average Q-value of agent for this episode: "+ ddqnAgent.getAvgQvalue()); //2022 new

            }
        }


        Date SimulationEndDate = Calendar.getInstance().getTime();
        now = df.format(SimulationEndDate);
        SimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));


    }
}

