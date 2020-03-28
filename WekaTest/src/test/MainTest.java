package test;

import weka.attributeSelection.*;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.evaluation.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SGD;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.LMT;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MainTest
{
    private static final String Fichier = "gamesAdvancedStats-StdDevs.arff";
    private static final String CheminAcces = "C:/Users/luco0/Desktop/Cours/s4/fouilleDonnees/arffs"; //Attention, les fichiers doivent être organiser suivant des dossiers nommé 2008, 2009 ...,  dans l'exemple ci contre, il doivent ce trouver dans le dossier arffs
    private static final int borneInf = 2008;
    private static final int borneMax = 2017;
    private static FileWriter fichier;
    private static FileWriter fichier2;

    public static void main(String[] args) throws Exception
    {
        long depart = System.currentTimeMillis();
        System.out.println("Depart à : depart");
        fichier = new FileWriter("resultat.txt");
        fichier2 = new FileWriter("selection.txt");

        ArrayList<int[]> removeLists = new ArrayList<>();
        HashMap<Integer,int[]> instancesWrapper = new HashMap<>();
        boolean traiCross = true;
        for(int k = 0; k<5;k++) {
            int[] removeList = null;
            switch (k)
            {
                case 0 : removeList = correlationAttributEv();
                         removeLists.add(removeList);
                         fichier.write("\nCorrelationAttributEval | Ranker");
                         fichier2.write("\n\nCorrelationAttributEval | Ranker\n");
                         System.out.println("Trie de données : CorrelationAttributEval | Ranker");
                         break;
                case 1 : removeList = cfsSubsetEv();
                         removeLists.add(removeList);
                         fichier.write("\nCfsSubsetEval | BestFirst");
                         fichier2.write("\n\nCfsSubsetEval | BestFirst\n");
                         System.out.println("Trie de données : CfsSubsetEval | BestFirst");
                         break;
                case 2 : removeList = infoGainAttributeEv();
                         removeLists.add(removeList);
                         fichier.write("\nInfoGainAttributeEval | Ranker");
                         fichier2.write("\n\nInfoGainAttributeEval | Ranker\n");
                         System.out.println("Trie de données : InfoGainAttributeEval | Ranker");
                         break;
                case 3 : removeList = null;
                         fichier.write("\nwrapperSubsetEval | GreedyStepwise");
                         fichier2.write("\n\nwrapperSubsetEval | GreedyStepwise\n");
                         System.out.println("Trie de données : wrapperSubsetEval | GreedyStepwise");
                         break;
                case 4 : removeList = null;
                         BestOfAll(instancesWrapper, removeLists);
                         fichier.write("\nallEvaluateur");
                         fichier2.write("\n\nallEvaluateur\n");
                         System.out.println("Trie de données : allEvaluateur");
                         break;
               default: removeList = null; break;
            }

            ArrayList<ArrayList<Double>> res = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                res.add(new ArrayList<>());
            }
            for (int annee = borneInf; annee < borneMax; annee++) {
                System.out.println("    " + annee + " en cours...");
                fichier.write("\n\n"+annee+"\n");
                allEvalAnnee(annee, removeList, res, traiCross, instancesWrapper);
                System.out.println("    " + "FIN : " + annee);
            }

            fichier.write("\n\nMoyenne\n");

            int indice = 0;
            for (ArrayList<Double> ar : res) {
                double moyenne = 0;
                double i = 0;
                for (Double rt : ar) {
                    i++;
                    moyenne += rt;
                }
                String nom;
                switch (indice) {
                    case 0:
                        nom = "     NaiveBayes";
                        break;
                    case 1:
                        nom = "     NaiveBayes with useKernelEstimator";
                        break;
                    case 2:
                        nom = "     SGD";
                        break;
                    case 3:
                        nom = "     J48 - 64";
                        break;
                    case 4:
                        nom = "     J48 - 128";
                        break;
                    case 5:
                        nom = "     J48 - 256";
                        break;
                    case 6:
                        nom = "     LMT";
                        break;
                    case 7:
                        nom = "     Logistic";
                        break;
                    case 8:
                        nom = "     IBk";
                        break;
                    case 9:
                        nom = "     SMO - 1.0";
                        break;
                    case 10:
                        nom = "     SMO - 2.0";
                        break;
                    case 11:
                        nom = "     SMO - 3.0";
                        break;
                    default:
                        nom = "";
                        break;
                }
                indice++;
                fichier.write((moyenne / i)+"\n");
                System.out.println(nom + " : " + (moyenne / i));
            }
        }
        fichier.close();
        fichier2.close();
        long fin = System.currentTimeMillis();
        System.out.println("Fin à : "+fin);
        System.out.println("Temps écoulé : "+(fin-depart));
    }

    private static void BestOfAll(HashMap<Integer, int[]> instancesWrapper, ArrayList<int[]> removeLists)
    {
        for(int i=0; i<12; i++) {
            ArrayList<Integer> fusion = new ArrayList<>();
            for (int[] tab : removeLists) {
                for (int entier : tab) {
                    if (!fusion.contains(entier)) {
                        fusion.add(entier);
                    }
                }
            }
            for(int entier : instancesWrapper.get(i))
            {
                if (!fusion.contains(entier)) {
                    fusion.add(entier);
                }
            }

            int[] remLi = new int[fusion.size()];
            for (int j = 0; j < fusion.size(); j++) {
                remLi[j] = fusion.get(j);
            }
            instancesWrapper.replace(i, remLi);
        }

    }

    /*public static int[] PrincipalCompo() throws Exception
    {
        Instances training = geneTraining("2008");

        PrincipalComponents principalComponents = new PrincipalComponents();
        Ranker ranker = new Ranker();
        AttributeSelection selector = new AttributeSelection();

        selector.setEvaluator(principalComponents);
        selector.setSearch(ranker);
        selector.SelectAttributes(training);

        System.out.println(Arrays.toString(selector.selectedAttributes()));

        int count = 0;
        for(double[] dbl:selector.rankedAttributes())
        {
            for(Double dubl:dbl)
            {
                if(dubl < 0.1)
                {
                    count++;
                }
            }
        }
        count = count+training.numAttributes()-selector.numberAttributesSelected()+1;

        int indice = 0;
        int i = 0;
        int removeList[] = new int[count];
        for(double[] dbl:selector.rankedAttributes())
        {
            for(Double dubl:dbl)
            {
                if(dubl > 1)
                {
                    indice = dubl.intValue();
                }
                if(dubl < 0.1)
                {
                    removeList[i] = indice;
                    i++;
                }
            }
        }

        for(int k = selector.numberAttributesSelected()-1; k<training.numAttributes(); k++)
        {
            removeList[i] = k;
            i++;
        }

        return removeList;
    }*/

    public static int[] wrapperSubsetEv(Classifier classifier) throws Exception
    {
        Instances data = geneBigData();
        WrapperSubsetEval wrapperSubsetEval = new WrapperSubsetEval();
        GreedyStepwise greedyStepwise = new GreedyStepwise();
        AttributeSelection selector = new AttributeSelection();

        wrapperSubsetEval.setClassifier(classifier);

        selector.setEvaluator(wrapperSubsetEval);
        selector.setSearch(greedyStepwise);
        selector.SelectAttributes(data);

        int[] selected = selector.selectedAttributes();
        int[] removelist = new int[data.size() - selected.length];
        int indice = 0;
        for(int i=0; i<data.numAttributes(); i++)
        {
            boolean remv = true;
            for(int entier:selected)
            {
                if(entier == i)
                {
                    remv = false;
                    break;
                }
            }
            if(remv)
            {
                removelist[indice] = i;
                indice++;
            }
        }

        return removelist;

    }

    public static int[] infoGainAttributeEv() throws Exception
    {
        Instances training = geneBigData();

        InfoGainAttributeEval infoGainAttributeEval = new InfoGainAttributeEval();
        Ranker ranker = new Ranker();
        AttributeSelection selector = new AttributeSelection();

        selector.setEvaluator(infoGainAttributeEval);
        selector.setSearch(ranker);
        selector.SelectAttributes(training);

        int count = 0;
        for(double[] dbl:selector.rankedAttributes())
        {
            for(Double dubl:dbl)
            {
                if(dubl < 0.02)
                {
                    count++;
                }
            }
        }

        int indice = 0;
        int i = 0;
        int removeList[] = new int[count];
        for(double[] dbl:selector.rankedAttributes())
        {
            for(Double dubl:dbl)
            {
                if(dubl > 1)
                {
                    indice = dubl.intValue();
                }
                if(dubl < 0.01)
                {
                    removeList[i] = indice;
                    i++;
                }
            }
        }

        return removeList;
    }

    public static int[] cfsSubsetEv() throws Exception
    {
        Instances training = geneBigData();

        CfsSubsetEval cfsSubsetEval = new CfsSubsetEval();
        BestFirst bestFirst = new BestFirst();
        AttributeSelection selector = new AttributeSelection();

        selector.setEvaluator(cfsSubsetEval);
        selector.setSearch(bestFirst);
        selector.SelectAttributes(training);

        int[] selected = selector.selectedAttributes();
        int[] removelist = new int[training.size() - selected.length];
        int indice = 0;
        for(int i=0; i<training.numAttributes(); i++)
        {
            boolean remv = true;
            for(int entier:selected)
            {
                if(entier == i)
                {
                    remv = false;
                    break;
                }
            }
            if(remv)
            {
                removelist[indice] = i;
                indice++;
            }
        }

        return removelist;
    }

    public static int[] correlationAttributEv() throws Exception {

        Instances training = geneBigData();

        CorrelationAttributeEval correlationAttributeEval = new CorrelationAttributeEval();
        Ranker ranker = new Ranker();
        AttributeSelection selector = new AttributeSelection();

        selector.setEvaluator(correlationAttributeEval);
        selector.setSearch(ranker);
        selector.SelectAttributes(training);

        int count = 0;
        for(double[] dbl:selector.rankedAttributes())
        {
            for(Double dubl:dbl)
            {
                if(dubl < 0.05 && dubl > -0.05)
                {
                    count++;
                }
            }
        }

        int indice = 0;
        int i = 0;
        int removeList[] = new int[count];
        for(double[] dbl:selector.rankedAttributes())
        {
            for(Double dubl:dbl)
            {
                if(dubl > 1)
                {
                    indice = dubl.intValue();
                }
                if(dubl < 0.05 && dubl > -0.05)
                {
                    removeList[i] = indice;
                    i++;
                }
            }
        }

        return removeList;
    }

    public static Instances geneBigData() throws Exception{
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(CheminAcces + "/" + borneInf + "/" + Fichier);
        Instances data = source.getDataSet();
        for(int i = borneInf+1; i<=borneMax; i++) {
            source = new ConverterUtils.DataSource(CheminAcces + "/" + i + "/" + Fichier);
            data.addAll(source.getDataSet());
        }
        Remove remove = geneRemove(data);
        //creation du jeu de training avec le filtre
        Instances training = Filter.useFilter(data, remove);
        int indice = 0;
        for(int i=0; i<training.numAttributes(); i++)
        {
            String name = training.attribute(i).name();
            if (name.equals("outcomeForFirst"))
            {
                indice=i;
                break;
            }
        }
        if (training.classIndex() == -1) {training.setClassIndex(indice) ;}
        return training;
    }

    public static Instances geneData(int annee) throws Exception {
        //Chargement d'un jeu de données
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(CheminAcces + "/" + borneInf + "/" + Fichier);
        Instances data = source.getDataSet();

        for(int i = borneInf+1; i<=annee; i++) {
            source = new ConverterUtils.DataSource(CheminAcces + "/" + i + "/" + Fichier);
            data.addAll(source.getDataSet());
        }

        Remove remove = geneRemove(data);
        //creation du jeu de training avec le filtre
        Instances training = Filter.useFilter(data, remove);

        int indice = 0;
        for(int i=0; i<training.numAttributes(); i++)
        {
            String name = training.attribute(i).name();
            if (name.equals("outcomeForFirst"))
            {
                indice=i;
                break;
            }
        }

        if (training.classIndex() == -1){ training.setClassIndex(indice) ;}
        return training;
    }


    public static Instances geneTraining(int annee) throws Exception {
        //Chargement d'un jeu de données

        ConverterUtils.DataSource source = new ConverterUtils.DataSource(CheminAcces + "/" + (annee+1) + "/" + Fichier);
        Instances data = source.getDataSet();
        for(int i = annee+2; i<=borneMax; i++) {
            source = new ConverterUtils.DataSource(CheminAcces + "/" + i + "/" + Fichier);
            data.addAll(source.getDataSet());
        }

        Remove remove = geneRemove(data);

        //creation du jeu de training avec le filtre
        Instances training = Filter.useFilter(data, remove);

        int indice = 0;
        for(int i=0; i<training.numAttributes(); i++)
        {
            String name = training.attribute(i).name();
            if (name.equals("outcomeForFirst"))
            {
                indice=i;
                break;
            }
        }

        if (training.classIndex() == -1){ training.setClassIndex(indice) ;}
        return training;
    }

    public static Instances removeList(Instances data, int[] removeList) throws Exception
    {
        //creation du filtre
        Remove remove = new Remove();
        //selection des attributs à supprimer
        remove.setAttributeIndicesArray(removeList);
        //s'applique sur le jeu de test data
        remove.setInputFormat(data);
        return Filter.useFilter(data, remove);
    }

    public static Remove geneRemove(Instances data) throws Exception
    {
        //selection des colones à supprimer
        int[] columnToUse = new int[3];
        int indice = 0;
        for(int i=0; i<data.numAttributes(); i++)
        {
            String name = data.attribute(i).name();
            if (name.equals("pointDifferenceForFirst") || name.equals("name1") || name.equals("name2"))
            {
                columnToUse[indice]=i;
                if(indice == 0)
                {
                    columnToUse[1]=i;
                    columnToUse[2]=i;
                }
                indice++;
            }
        }

        //creation du filtre
        Remove remove = new Remove();
        //selection des attributs à supprimer
        remove.setAttributeIndicesArray(columnToUse);
        //s'applique sur le jeu de test data
        remove.setInputFormat(data);
        return remove;
    }

    public static void allEvalAnnee(int annee,int[] removeList, ArrayList<ArrayList<Double>> resultat, Boolean trainCross, HashMap<Integer,int[]> instancesWrapper) throws Exception {
        Instances data = geneData(annee);
        Instances Training = geneTraining(annee);

        Boolean wrapper = false;
        if(removeList != null) {
            data = removeList(data, removeList);
            Training = removeList(Training, removeList);

            if (annee == borneInf) {
                for (int i = 0; i < data.numAttributes(); i++) {
                    fichier2.write(data.attribute(i).name()+"\n");
                    System.out.println("         " + data.attribute(i).name());
                }
            }
        }
        else
        {
            wrapper = true;
        }

        Instances finalTraining = Training;
        Instances finalData = data;
        AtomicInteger finit = new AtomicInteger();

        Boolean finalWrapper = wrapper;
        Thread NaiveBayes = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationNaiveBayes(finalData, false, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){
                System.out.println(e);
            };
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        NaiveBayes  Rate : "+res);
            resultat.get(0).add(res);
            finit.set(finit.get() + 1);
        });

        Thread NaiveBayes2 = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationNaiveBayes(finalData, true, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        NaiveBayes with useKernelEstimator Rate : "+res);
            resultat.get(1).add(res);
            finit.set(finit.get() + 1);
        });

        Thread SGD = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationSGD(finalData, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        SGD Rate : "+res);
            resultat.get(2).add(res);
            finit.set(finit.get() + 1);
        });

        Thread J4864 = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationJ48(finalData, 64, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        J48 - 64 Rate : "+res);
            resultat.get(3).add(res);
            finit.set(finit.get() + 1);
        });

        Thread J48128 = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationJ48(finalData, 128, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        J48 - 128 Rate : "+res);
            resultat.get(4).add(res);
            finit.set(finit.get() + 1);
        });

        Thread J48256 = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationJ48(finalData, 256, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        J48 - 256 Rate : "+res);
            resultat.get(5).add(res);
            finit.set(finit.get() + 1);
        });

        Thread LMT = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationLMT(finalData, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        LMT Rate : "+res);
            resultat.get(6).add(res);
            finit.set(finit.get() + 1);
        });

        Thread Logistic = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationLogistic(finalData, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        Logistic Rate : "+res);
            resultat.get(7).add(res);
            finit.set(finit.get() + 1);
        });

        Thread IBK = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationIBK(finalData, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        IBk Rate : "+res);
            resultat.get(8).add(res);
            finit.set(finit.get() + 1);
        });

        Thread SMO1 = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationSMO(finalData, 1.0, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        SMO - 1.0 Rate : "+res);
            resultat.get(9).add(res);
            finit.set(finit.get() + 1);
        });

        Thread SMO2 = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationSMO(finalData, 2.0, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        SMO - 2.0 Rate : "+res);
            resultat.get(10).add(res);
            finit.set(finit.get() + 1);
        });

        Thread SMO3 = new Thread(() -> {
            double res = -1;
            try {
                res = evaluationSMO(finalData, 3.0, finalWrapper, instancesWrapper, finalTraining, annee);
            }catch (Exception e){};
            try {
                fichier.write(res+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("        SMO - 3.0 Rate : "+res);
            resultat.get(11).add(res);
            finit.set(finit.get() + 1);
        });
        NaiveBayes.start();
        NaiveBayes2.start();
        SGD.start();
        J4864.start();
        J48128.start();
        J48256.start();
        LMT.start();
        Logistic.start();
        IBK.start();
        SMO1.start();
        SMO2.start();
        SMO3.start();
        while(finit.intValue() != 12);
    }

    public static double evaluationNaiveBayes(Instances data, Boolean withUseKernelOption, Boolean wrapper, HashMap<Integer,int[]> instancesWrapper, Instances training, int annee) throws Exception {

        double moyenne = 0;
        //creation du classifieur --------- naivebayes ---------
        NaiveBayes naiveBayes = new NaiveBayes();

        int indice = 0;
        if (withUseKernelOption) {
            indice = 1;
            //Mise en place des options
            String[] options = new String[1];
            options[0] = "-K";

            //Ajout des options sur le classifieur
            naiveBayes.setOptions(options);
        }

        if(wrapper)
        {
            if(!instancesWrapper.containsKey(indice))
            {
                instancesWrapper.put(indice,wrapperSubsetEv(naiveBayes));
            }
            data = removeList(data, instancesWrapper.get(indice));
            training = removeList(training, instancesWrapper.get(indice));
            if(annee == borneInf)
            {
                for (int i = 0; i < data.numAttributes(); i++) {
                    fichier2.write("NaiveBayes "+data.attribute(i).name()+"\n");
                    System.out.println("         NaiveBayes "+indice+" "+ data.attribute(i).name());
                }
            }
        }


        //mise du classifieur sur le jeu de training
        naiveBayes.buildClassifier(data);
        for(int i = 0; i < 10; i++) {
            //evaluation du classifieur --------- naivebayes ---------

            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(naiveBayes, training, 10, new Random(i));
            moyenne += (1-eval.errorRate());
        }
        return (moyenne/10);
    }

    public static double evaluationSGD(Instances data, Boolean wrapper, HashMap<Integer,int[]> instancesWrapper, Instances training, int annee) throws Exception {
        double moyenne = 0;
        SGD sgd = new SGD();

        if(wrapper)
        {
            int indice = 2;
            if(!instancesWrapper.containsKey(indice))
            {
                instancesWrapper.put(indice,wrapperSubsetEv(sgd));
            }
            data = removeList(data, instancesWrapper.get(indice));
            training = removeList(training, instancesWrapper.get(indice));
            if(annee == borneInf)
            {
                for (int i = 0; i < data.numAttributes(); i++) {
                    fichier2.write("SGD "+data.attribute(i).name()+"\n");
                    System.out.println("         SGD "+indice+" "+ data.attribute(i).name());
                }
            }
        }

        sgd.buildClassifier(data);
        for(int i = 0; i < 10; i++) {
            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(sgd, training,10,new Random(i));
            moyenne += (1-eval.errorRate());
        }
        return (moyenne/10);
    }

    public static double evaluationJ48(Instances data, int minNumObj, Boolean wrapper, HashMap<Integer,int[]> instancesWrapper, Instances training, int annee) throws Exception {
        double moyenne = 0;
        J48 j48 = new J48();

        j48.setMinNumObj(minNumObj);

        if(wrapper)
        {
            int indice = 3;
            switch (minNumObj)
            {
                case 64: indice = 3; break;
                case 128: indice = 4; break;
                case 256: indice = 5; break;
            }
            if(!instancesWrapper.containsKey(indice))
            {
                instancesWrapper.put(indice,wrapperSubsetEv(j48));
            }
            data = removeList(data, instancesWrapper.get(indice));
            training = removeList(training, instancesWrapper.get(indice));
            if(annee == borneInf)
            {
                for (int i = 0; i < data.numAttributes(); i++) {
                    fichier2.write("J48 "+data.attribute(i).name()+"\n");
                    System.out.println("         J48 "+indice+" "+ data.attribute(i).name());
                }
            }
        }

        j48.buildClassifier(data);
        for(int i = 0; i < 10; i++) {
            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(j48, training,10,new Random(i));
            moyenne += (1-eval.errorRate());
        }
        return (moyenne/10);
    }

    public static double evaluationLMT(Instances data, Boolean wrapper, HashMap<Integer,int[]> instancesWrapper, Instances training, int annee) throws Exception {
        AtomicReference<Double> moyenne = new AtomicReference<>((double) 0);
        LMT lmt = new LMT();

        if(wrapper)
        {
            int indice = 6;
            if(!instancesWrapper.containsKey(indice))
            {
                instancesWrapper.put(indice,wrapperSubsetEv(lmt));
            }
            data = removeList(data, instancesWrapper.get(indice));
            training = removeList(training, instancesWrapper.get(indice));
            if(annee == borneInf)
            {
                for (int i = 0; i < data.numAttributes(); i++) {
                    fichier2.write("LMT "+data.attribute(i).name()+"\n");
                    System.out.println("         LMT "+indice+" "+ data.attribute(i).name());
                }
            }
        }

        lmt.buildClassifier(data);
        AtomicInteger fin = new AtomicInteger();
        for(int i = 0; i < 10; i++) {
            int finalI = i;
            Instances finalTraining = training;
            Instances finalData = data;
            Thread t = new Thread(() -> {
                Evaluation eval = null;
                try {
                    eval = new Evaluation(finalData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    eval.crossValidateModel(lmt, finalTraining, 10, new Random(finalI));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Evaluation finalEval = eval;
                moyenne.updateAndGet(v -> new Double((double) (v + (1 - finalEval.errorRate()))));
                fin.getAndIncrement();
            });
            t.start();
        }
        while (fin.intValue() != 10);
        return (moyenne.get() /10);
    }

    public static double evaluationLogistic(Instances data, Boolean wrapper, HashMap<Integer,int[]> instancesWrapper, Instances training, int annee) throws Exception {
        double moyenne = 0;
        Logistic logistic = new Logistic();

        if(wrapper)
        {
            int indice = 7;
            if(!instancesWrapper.containsKey(indice))
            {
                instancesWrapper.put(indice,wrapperSubsetEv(logistic));
            }
            data = removeList(data, instancesWrapper.get(indice));
            training = removeList(training, instancesWrapper.get(indice));
            if(annee == borneInf)
            {
                for (int i = 0; i < data.numAttributes(); i++) {
                    fichier2.write("Logistic "+data.attribute(i).name()+"\n");
                    System.out.println("         Logistic "+indice+" "+ data.attribute(i).name());
                }
            }
        }

        logistic.buildClassifier(data);
        for(int i = 0; i < 10; i++) {
            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(logistic, training,10,new Random(i));
            moyenne += (1-eval.errorRate());
        }
        return (moyenne/10);
    }

    public static double evaluationIBK(Instances data, Boolean wrapper, HashMap<Integer,int[]> instancesWrapper, Instances training, int annee) throws Exception {
        double moyenne = 0;
        IBk ibk = new IBk();

        if(wrapper)
        {
            int indice = 8;
            if(!instancesWrapper.containsKey(indice))
            {
                instancesWrapper.put(indice,wrapperSubsetEv(ibk));
            }
            data = removeList(data, instancesWrapper.get(indice));
            training = removeList(training, instancesWrapper.get(indice));
            if(annee == borneInf)
            {
                for (int i = 0; i < data.numAttributes(); i++) {
                    fichier2.write("IBK "+data.attribute(i).name()+"\n");
                    System.out.println("         IBK "+indice+" "+ data.attribute(i).name());
                }
            }
        }

        ibk.buildClassifier(data);
        for(int i = 0; i < 10; i++) {
            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(ibk, training,10,new Random(i));
            moyenne += (1-eval.errorRate());
        }
        return (moyenne/10);
    }

    public static double evaluationSMO(Instances data, double optionc, Boolean wrapper, HashMap<Integer,int[]> instancesWrapper, Instances training, int annee) throws Exception {
        double moyenne = 0;
        SMO smo = new SMO();

        smo.setC(optionc);

        if(wrapper)
        {
            int indice = 9;
            switch ((int) optionc)
            {
                case 1: indice = 9;break;
                case 2: indice = 10;break;
                case 3: indice = 11;break;
            }
            if(!instancesWrapper.containsKey(indice))
            {
                instancesWrapper.put(indice,wrapperSubsetEv(smo));
            }
            data = removeList(data, instancesWrapper.get(indice));
            training = removeList(training, instancesWrapper.get(indice));
            if(annee == borneInf)
            {
                for (int i = 0; i < data.numAttributes(); i++) {
                    fichier2.write("SMO "+data.attribute(i).name()+"\n");
                    System.out.println("         SMO "+indice+" "+ data.attribute(i).name());
                }
            }
        }

        smo.buildClassifier(data);
        for(int i = 0; i < 10; i++) {
            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(smo, training,10,new Random(i));
            moyenne += (1-eval.errorRate());
        }
        return (moyenne/10);
    }
}
