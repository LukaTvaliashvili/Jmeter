import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSampler;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class Jmeter_search_tests {

    static Map<String, String> properties = getProperties();

    private static final String jmeter_folder = properties.get("jmeter_folder");

    private static final String index_name = properties.get("index");

    private static String searchType;

    private static final String structure = properties.get("structure");

    /* metric should have one of three values: "tanimoto", "tversky", "euclid-cub". For "tversky" alpha and beta values are essential.
     For others, it does not have any effect, so default value of 0.0f can be used. */

    private static String metric;

    //minVal and maxVal are vital for similarity search it should range from 0 to 1, it does not affect other searches

    private static final float minVal = Float.parseFloat(properties.get(("minval")));

    private static final float maxVal = Float.parseFloat(properties.get(("maxval")));

    private static final float alpha = Float.parseFloat(properties.get(("alpha")));

    private static final float beta = Float.parseFloat(properties.get(("beta")));

    private static final int users = Integer.parseInt(properties.get("users"));

    private static final int rampup = Integer.parseInt(properties.get("rampup"));

    private static File jmeterHome = new File(jmeter_folder);

    private static String slash = System.getProperty("file.separator");

    private static File jmeterProperties = new File(jmeterHome.getPath() + slash + "bin" + slash + "jmeter.properties");

    private static StandardJMeterEngine jmeter = new StandardJMeterEngine();

    private static String jtl_result_folder = properties.get("jtl_result_folder");

    private static String jmx_config_folder = properties.get("jmx_config_folder");


    static {
        //JMeter initialization (properties.txt, log levels, locale, etc)
        JMeterUtils.setJMeterHome(jmeterHome.getPath());
        JMeterUtils.loadJMeterProperties(jmeterProperties.getPath());
        JMeterUtils.setProperty("jmeter.save.saveservice.output_format", "xml");
        JMeterUtils.setProperty("jmeter.save.saveservice.response_data", "true");
        JMeterUtils.initLogging();// you can comment this line out to see extra log messages of i.e. DEBUG level
        JMeterUtils.initLocale();
    }


    public static void main(String[] args) {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
        df.format(new Date());

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the type of search to be tested (exact, sim, or sub): ");

        String temp = scanner.nextLine().toLowerCase();

        if (temp.equals("exact") || temp.equals("sim") || temp.equals("sub")) {
            searchType = temp;
        } else {
            System.out.println("Input is not valid, try again");
            return;
        }

        if (searchType.equals("sim")) {
            System.out.println("Select the desired metric for similarity search (tanimoto, tversky, euclid-cub): ");
            String tmp = scanner.nextLine();
            if (tmp.equals("tanimoto") || tmp.equals("tversky") || tmp.equals("euclid-cub")) {
                metric = tmp;
            } else {
                System.out.println("Input is not valid, try again");
                return;
            }
            scanner.close();
        }

        switch (searchType) {
            case "exact":
                exact_search_test(users, rampup, structure, jtl_result_folder + "/" + df.format(new Date()) + "_" + users + "_" + rampup + "_" + searchType + ".jtl",
                        jmx_config_folder + "/" + df.format(new Date()) + "_" + "users" + users + "_" + "rampup" + rampup + "_" + searchType + ".jmx");
                break;
            case "sub":
                sub_search_test(users, rampup, structure, jtl_result_folder + "/" + df.format(new Date()) + "_" + users + "_" + rampup + "_" + searchType + ".jtl",
                        jmx_config_folder + "/" + df.format(new Date()) + "_" + "users" + users + "_" + "rampup" + rampup + "_" + searchType + ".jmx");
                break;
            case "sim":
                similarity_search_test(users, rampup, structure, jtl_result_folder + "/" + df.format(new Date()) + "_" + users + "_" + rampup + "_" + searchType + "_" + metric + ".jtl",
                        jmx_config_folder + "/" + df.format(new Date()) + "_" + "users" + users + "_" + "rampup" + rampup + "_" + searchType + "_" + metric + ".jmx");

        }

    }


    public static void exact_search_test(int number_of_users, int ramp_up_period, String exact_structure,
                                         String where_to_export_jtl_results_file,
                                         String where_to_export_jmx_config_file) {

        if (jmeterHome.exists()) {

            if (jmeterProperties.exists()) {

                HeaderManager exact_search_header = getHeaderManager();

                HTTPSampler exact_search_request = getHttpSampler(exact_structure, "ExactSearch", "exact", metric, minVal, maxVal, alpha, beta);


                LoopController exact_search_loopController = getLoopController();

                // Thread Group
                ThreadGroup threadGroup1 = getThreadGroup(number_of_users, ramp_up_period, exact_search_loopController);

                // Test Plan
                TestPlan testPlan = new TestPlan("Exact Search");
                testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
                testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
                testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());

                //Highest node in hirerachy
                HashTree testPlanTree = new HashTree();

                // Construct Test Plan from previously initialized elements
                testPlanTree.add(testPlan);
                HashTree threadGroupHashTree1 = testPlanTree.add(testPlan, threadGroup1);
                threadGroupHashTree1.add(exact_search_request, exact_search_header);

                // save generated test plan to JMeter's .jmx file format
                output_files_config(where_to_export_jtl_results_file, where_to_export_jmx_config_file, testPlanTree);

                // Run Test Plan
                jmeter.configure(testPlanTree);
                jmeter.run();

                System.out.println("Test completed. See " + where_to_export_jtl_results_file + " file for results");
                System.out.println("JMeter .jmx script is available at " + where_to_export_jmx_config_file);
                System.exit(0);

            }
        }

        System.err.println("jmeter.home property is not set or pointing to incorrect location");
        System.exit(1);
    }

    public static void similarity_search_test(int number_of_users, int ramp_up_period, String sim_structure,
                                              String where_to_export_jtl_results_file,
                                              String where_to_export_jmx_config_file) {
        if (jmeterHome.exists()) {

            if (jmeterProperties.exists()) {
                HeaderManager similarity_search_header = getHeaderManager();

                HTTPSampler similarity_search_request = getHttpSampler(sim_structure, "SimilaritySearch", "sim", metric, minVal, maxVal, alpha, beta);

                //Loop controller is must in jmeter java code.
                LoopController similarity_search_loopController = getLoopController();

                // Thread Group
                ThreadGroup threadGroup = getThreadGroup(number_of_users, ramp_up_period, similarity_search_loopController);

                // Test Plan
                TestPlan testPlan = new TestPlan("Sim Search");
                testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
                testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
                testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());

                //Highest node in hirerachy
                HashTree testPlanTree = new HashTree();

                // Construct Test Plan from previously initialized elements
                testPlanTree.add(testPlan);
                HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
                threadGroupHashTree.add(similarity_search_request, similarity_search_header);


                output_files_config(where_to_export_jtl_results_file, where_to_export_jmx_config_file, testPlanTree);


                // Run Test Plan
                jmeter.configure(testPlanTree);
                jmeter.run();

                System.out.println("Test completed. See " + where_to_export_jtl_results_file + " file for results");
                System.out.println("JMeter .jmx script is available at " + where_to_export_jmx_config_file);
                System.exit(0);

            }
        }

        System.err.println("jmeter.home property is not set or pointing to incorrect location");
        System.exit(1);
    }


    public static void sub_search_test(int number_of_users, int ramp_up_period, String exact_structure,
                                       String where_to_export_jtl_results_file,
                                       String where_to_export_jmx_config_file) {

        if (jmeterHome.exists()) {

            if (jmeterProperties.exists()) {

                HeaderManager sub_search_header = getHeaderManager();

                HTTPSampler sub_search_request = getHttpSampler(exact_structure, "SubSearch", "sub", metric, minVal, maxVal, alpha, beta);


                LoopController sub_search_loopController = getLoopController();

                // Thread Group
                ThreadGroup threadGroup1 = getThreadGroup(number_of_users, ramp_up_period, sub_search_loopController);

                // Test Plan
                TestPlan testPlan = new TestPlan("Sub Search");
                testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
                testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
                testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());

                //Highest node in hirerachy
                HashTree testPlanTree = new HashTree();

                // Construct Test Plan from previously initialized elements
                testPlanTree.add(testPlan);
                HashTree threadGroupHashTree1 = testPlanTree.add(testPlan, threadGroup1);
                threadGroupHashTree1.add(sub_search_request, sub_search_header);

                // save generated test plan to JMeter's .jmx file format
                output_files_config(where_to_export_jtl_results_file, where_to_export_jmx_config_file, testPlanTree);

                // Run Test Plan
                jmeter.configure(testPlanTree);
                jmeter.run();

                System.out.println("Test completed. See " + where_to_export_jtl_results_file + " file for results");
                System.out.println("JMeter .jmx script is available at " + where_to_export_jmx_config_file);
                System.exit(0);

            }
        }

        System.err.println("jmeter.home property is not set or pointing to incorrect location");
        System.exit(1);
    }

    public static void sub_search_test_with_external_jmx(int number_of_users, String sub_structure,
                                                         String where_to_export_jtl_results_file,
                                                         String jmx_config_file) throws IOException {

        // Load existing .jmx Test Plan
        HashTree testPlanTree = SaveService.loadTree(new File(jmx_config_file));

        output_files_config(where_to_export_jtl_results_file, null, testPlanTree);
        // Run JMeter Test
        jmeter.configure(testPlanTree);
        jmeter.run();
    }


    private static LoopController getLoopController() {
        LoopController search_loopController = new LoopController();
        search_loopController.setLoops(1);
        search_loopController.setFirst(true);
        search_loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        search_loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
        search_loopController.initialize();
        return search_loopController;
    }

    private static HTTPSampler getHttpSampler(String structure, String name, String searchType,
                                              String metric, float minVal, float maxVal, float alpha, float beta) {
        HTTPSampler sub_search_request = new HTTPSampler();
        sub_search_request.setDomain("localhost");
        sub_search_request.setPort(8080);
        sub_search_request.setPath("/search");
        sub_search_request.setMethod("POST");
        sub_search_request.setName(name);
        sub_search_request.setHeaderManager(getHeaderManager());
        sub_search_request.addNonEncodedArgument("", "{\n" +
                "    \"libraries\": [\n" +
                "        {\n" +
                "            \"storage\": \"elastic\",\n" +
                "            \"library_ids\": [\n" +
                "                \"" + index_name + "\"\n" +
                "            ]\n" +
                "        }\n" +
                "    ],\n" +
                "    \"similarity\": {\n" +
                "        \"metric\": \"" + metric + "\",\n" +
                "        \"min\": " + minVal + ",\n" +
                "        \"max\": " + maxVal + ",\n" +
                "        \"parameters\": {\n" +
                "            \"alpha\": " + alpha + ",\n" +
                "            \"beta\": " + beta + "\n" +
                "        }\n" +
                "    },\n" +
                "    \"hydrogen_visible\": false,\n" +
                "    \"query_structure\": \"" + structure + "\",\n" +
                "    \"type\": \"" + searchType + "\",\n" +
                "    \"offset\": 0,\n" +
                "    \"limit\": 20,\n" +
                "    \"options\": \"\",\n" +
                "    \"query_text\": \"\"\n" +
                "}", "");

        sub_search_request.setPostBodyRaw(true);
        sub_search_request.setProperty(TestElement.TEST_CLASS, HTTPSampler.class.getName());
        sub_search_request.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        return sub_search_request;
    }

    private static void output_files_config(String where_to_export_jtl_results_file,
                                            String where_to_export_jmx_config_file,
                                            HashTree testPlanTree) {
        // save generated test plan to JMeter's .jmx file format
        if (where_to_export_jmx_config_file != null) {
            try {
                SaveService.saveTree(testPlanTree, new FileOutputStream(where_to_export_jmx_config_file));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //add Summarizer output to get test progress in stdout like:
        // summary =      2 in   1.3s =    1.5/s Avg:   631 Min:   290 Max:   973 Err:     0 (0.00%)
        Summariser summer = null;
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (summariserName.length() > 0) {
            summer = new Summariser(summariserName);
        }


        // Store execution results into a .jtl file
        ResultCollector logger = new ResultCollector(summer);
        logger.setFilename(where_to_export_jtl_results_file);
        testPlanTree.add(testPlanTree.getArray()[0], logger);
    }

    private static ThreadGroup getThreadGroup(int number_of_users, int ramp_up_period, LoopController similarity_search_loopController) {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Thread Group 1");
        threadGroup.setNumThreads(number_of_users);
        threadGroup.setRampUp(ramp_up_period);
        threadGroup.setSamplerController(similarity_search_loopController);
        threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());
        return threadGroup;
    }

    private static HeaderManager getHeaderManager() {
        HeaderManager search_header = new HeaderManager();
        search_header.add(new Header("Content-Type", "application/json"));
        search_header.setProperty(TestElement.TEST_CLASS, HeaderManager.class.getName());
        search_header.setProperty(TestElement.GUI_CLASS, HeaderPanel.class.getName());
        return search_header;
    }

    private static Map<String, String> getProperties() {
        Map<String, String> map = new HashMap<>();
        try {
            Scanner myReader = new Scanner(new FileInputStream("src/main/resources/properties.txt"));
            while (myReader.hasNextLine()) {
                String[] split = myReader.nextLine()
                        .split(":");
                map.put(split[0], split[1]);
            }
            return map;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}

