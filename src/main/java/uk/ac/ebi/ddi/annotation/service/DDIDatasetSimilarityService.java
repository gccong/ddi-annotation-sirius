package uk.ac.ebi.ddi.annotation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import uk.ac.ebi.ddi.service.db.model.similarity.*;
import uk.ac.ebi.ddi.service.db.service.similarity.DatasetStatInfoService;
import uk.ac.ebi.ddi.service.db.service.similarity.ExpOutputDatasetService;
import uk.ac.ebi.ddi.service.db.service.similarity.TermInDBService;

import java.util.*;

/**
 * Created by mingze on 11/09/15.
 */
public class DDIDatasetSimilarityService {
    @Autowired
    TermInDBService termInDBService = new TermInDBService();
    @Autowired
    ExpOutputDatasetService expOutputDatasetService = new ExpOutputDatasetService();
    @Autowired
    DatasetStatInfoService datasetStatInfoService= new DatasetStatInfoService();

    private double[][] cosineScores = null;
    private List<ExpOutputDataset> expOutputDatasets = null;
    private HashMap<String, Integer> indecies = null;
    private List<TermInDB> termsInDB;
    private HashMap<String, Double> idfWeightMap;
    private String dataType;


//    public DDIExpDataProcessService(String dataType) {
//        this.expOutputDatasets = this.expOutputDatasetService.readAllInOneType(dataType);
//        this.termsInDB = termInDBService.readAllInOneType(dataType);
//        this.dataType = dataType;
//    }

    /**
     * default datatype
     */
    public DDIDatasetSimilarityService() {
        this.idfWeightMap = new HashMap<>();
    }

    /**
     * Calculate the inverse dataset/document frequency weight of each term = log(N/DatasetFrequency)
     *
     * @param dataType Omics type of the dataset
     */
    public void calculateIDFWeight(String dataType) {

        this.dataType = dataType;
        this.expOutputDatasets = this.expOutputDatasetService.readAllInOneType(dataType);
        this.termsInDB = termInDBService.readAllInOneType(dataType);

        long numberOfDatasets = expOutputDatasets.size();
        double tempscore;

        System.out.println("start to calculate IDFWeight for" + dataType);
        for (TermInDB term : termsInDB) {
            tempscore = (double) numberOfDatasets / (double) term.getDatasetFrequency();
            double idfWeigt = Math.log(tempscore) / Math.log(2);
            this.idfWeightMap.put(term.getTermName(), idfWeigt);
//            term.setIdfWeight(idfWeigt);   //here we have to write it to the termStatInfo, just creat a new termStatInfo;
//            termInDBService.update(term);
        }
        System.out.println("end of calculating IDFWeight for" + dataType);
    }

    /**
     * Calculate the intersection/(terms sharing) information between datasets
     *
     * @param dataType omics type of the dataset
     */
    public void calculateSimilarity(String dataType) {

        if (!dataType.equals(this.dataType)) {
            throw new IllegalStateException("The dataType for calculate Intersections is" + dataType + ", not same as prev calculated dataType" + this.dataType);
        }

        System.out.println("start to calculate similarity for" + dataType);

        long numberOfDatasets = expOutputDatasets.size();
        System.out.println("The number of Datasets for calculate Intersections:" + numberOfDatasets);

        int i = 0;
        this.indecies = new HashMap<String, Integer>();
        for (ExpOutputDataset dataset : expOutputDatasets) {
            this.indecies.put(dataset.getAccession(), i++);
        }


        this.cosineScores = calculateCosineScore(expOutputDatasets, numberOfDatasets);

        for (ExpOutputDataset dataset : expOutputDatasets) {
            List<IntersectionInfo> datasetIntersectionInfos = new ArrayList<>();
            List<TermInList> terms = dataset.getTerms();
            for (TermInList term : terms) {
                List<ExpOutputDataset> relatedDatasets = getRelatedDatasets(dataset, term, expOutputDatasets);
                List<IntersectionInfo> tempIntersectionInfos = getIntersectionInfos(term, dataset, relatedDatasets);
                datasetIntersectionInfos.addAll(tempIntersectionInfos);
            }

//            dataset.setTerms(terms);
            datasetIntersectionInfos = mergeIntersectionInfos(datasetIntersectionInfos);

            DatasetStatInfo datasetStatInfo = new DatasetStatInfo(dataset.getAccession(), "unkownDatabase", dataType, datasetIntersectionInfos);
            datasetStatInfoService.insert(datasetStatInfo);
        }
        System.out.println("end of calculating similarity for" + dataType);
    }

    /**
     * If one dataset share multiple terms with another dataset, merge the multiple Intersection Informations into one and set the right SharedTermNo
     *
     * @param datasetIntersectionInfos
     * @return
     */
    private List<IntersectionInfo> mergeIntersectionInfos(List<IntersectionInfo> datasetIntersectionInfos) {
        List<IntersectionInfo> newIntersectionInfos = new ArrayList<>();
        for (IntersectionInfo tempIntersectionInfo : datasetIntersectionInfos) {
            int i = contains(newIntersectionInfos, tempIntersectionInfo);
            if (i >= 0) {
                newIntersectionInfos.get(i).increaseOneSharedTermsNo();
            } else {
                tempIntersectionInfo.setSharedTermsNo(1);
                newIntersectionInfos.add(tempIntersectionInfo);
            }
        }
        return newIntersectionInfos;
    }

    /**
     * locate the index of intersectionInfo in the new merged intersectionInfo list
     *
     * @param datasetIntersectionInfos
     * @param tempIntersectionInfo
     * @return
     */
    private int contains(List<IntersectionInfo> datasetIntersectionInfos, IntersectionInfo tempIntersectionInfo) {
        for (int i = 0; i < datasetIntersectionInfos.size(); i++) {
            if (datasetIntersectionInfos.get(i).getRelatedDatasetAcc().equals(tempIntersectionInfo.getRelatedDatasetAcc())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get intersectionInfos from related datasets, which linked by term
     *
     * @param term
     * @param dataset
     * @param relatedDatasets
     * @return
     */
    private List<IntersectionInfo> getIntersectionInfos(TermInList term, ExpOutputDataset dataset, List<ExpOutputDataset> relatedDatasets) {
        List<IntersectionInfo> intersectionInfos = new ArrayList<IntersectionInfo>();
        int indexOfThisDataset = this.indecies.get(dataset.getAccession());
        for (ExpOutputDataset relateddataset : relatedDatasets) {
            int indexOfThatDataset = this.indecies.get(relateddataset.getAccession());
            IntersectionInfo intersectionInfo = new IntersectionInfo();
            intersectionInfo.setRelatedDatasetAcc(relateddataset.getAccession());
            intersectionInfo.setCosineScore(this.cosineScores[indexOfThisDataset][indexOfThatDataset]);
            intersectionInfos.add(intersectionInfo);
        }
        return intersectionInfos;
    }

    /**
     * calculate cosine scores between experiment output datasets
     *
     * @param expOutputDatasets
     * @param numberOfDatasets  total number of datasets(in one omics type)
     * @return score array
     */
    private double[][] calculateCosineScore(List<ExpOutputDataset> expOutputDatasets, long numberOfDatasets) {

        double[][] cosineScores = new double[(int) numberOfDatasets][(int) numberOfDatasets];
        List<TermInList> intersectionTerms = null;
        HashMap<String, Double> normMap = CalculateNormArray();

        for (ExpOutputDataset dataset : expOutputDatasets) {
            int indexOfThisDataset = this.indecies.get(dataset.getAccession());
            List<TermInList> termsOfThisDataset = dataset.getTerms();
            for (ExpOutputDataset dataset2 : expOutputDatasets) {
                int indexOfThatDataset = this.indecies.get(dataset2.getAccession());
                List<TermInList> termsOfThatDataset = dataset2.getTerms();
                if (termsOfThisDataset == termsOfThatDataset) { //same dataset
                    cosineScores[indexOfThisDataset][indexOfThatDataset] = -1;
                    continue;
                } else {
                    if (cosineScores[indexOfThisDataset][indexOfThatDataset] != 0) {//already calculated
                        continue;
                    }
                    intersectionTerms = getIntersectionSet(termsOfThisDataset, termsOfThatDataset);
                    if (intersectionTerms.size() == 0) {
                        cosineScores[indexOfThisDataset][indexOfThatDataset] = -1;
                        cosineScores[indexOfThatDataset][indexOfThisDataset] = -1;
                        continue;
                    }
                    double score = 0;
                    for (TermInList termInList : intersectionTerms) {
                        String termName = termInList.getTermName();
                        score += Math.pow(idfWeightMap.get(termName), 2); //each term has same score in both vector(dataset)
                    }
                    double scoreFinal = score / (normMap.get(dataset.getAccession()) * normMap.get(dataset2.getAccession()));
                    cosineScores[indexOfThisDataset][indexOfThatDataset] = scoreFinal;
                    cosineScores[indexOfThatDataset][indexOfThisDataset] = scoreFinal;
//                    if(intersectionTerms.size()>1) {
//                        System.out.println(dataset.getAccession() + "share with" + dataset2.getAccession() + ":" + intersectionTerms.size());
//                    }
                }
            }
        }

        return cosineScores;
    }

    private HashMap<String, Double> CalculateNormArray() {
        HashMap<String, Double> normMap = new HashMap<String, Double>();

        for (ExpOutputDataset dataset : expOutputDatasets) {
            double norm = 0;
            for (TermInList term : dataset.getTerms()) {
                double weight = idfWeightMap.get(term.getTermName());
                norm += Math.pow(weight, 2);
            }
            double normFinal = Math.sqrt(norm);
            normMap.put(dataset.getAccession(), normFinal);
        }
        return normMap;
    }

    private List<ExpOutputDataset> getRelatedDatasets(ExpOutputDataset originDataset, TermInList term, List<ExpOutputDataset> expOutputDatasets) {
        List<ExpOutputDataset> relatedDatasets = new ArrayList<ExpOutputDataset>();
        for (ExpOutputDataset possibleDataset : expOutputDatasets) {
            if (possibleDataset.getAccession().equals(originDataset.getAccession())) {
                continue;
            }
            List<TermInList> termsInList = possibleDataset.getTerms();
            for (TermInList tempTerm : termsInList) {
                if (tempTerm.getTermName().equals(term.getTermName())) {
                    relatedDatasets.add(possibleDataset);
                    break;
                }
            }
        }
        return relatedDatasets;
    }

    private static List<TermInList> getIntersectionSet(List<TermInList> set1, List<TermInList> set2) {
        List<TermInList> cloneSet = new ArrayList<TermInList>();
        for (TermInList termInList : set1) {
            for (TermInList termInList2 : set2) {
                if (termInList.getTermName().equals(termInList2.getTermName())) {
                    cloneSet.add(termInList);
                }

            }

        }
        return cloneSet;
    }


}