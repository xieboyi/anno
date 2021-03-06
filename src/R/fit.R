# first need to install rstan;  see install wiki online:
#   http://mc-stan.org/r-quickstart.html
library(rstan);

max_index <- function(v) {
  max_index <- 1;
  for (n in 2:length(v))
    if(v[n] > v[max_index])
      max_index <- n;
  return(max_index);
}


# assume data looks like
# question,rater,judgment
# 1,1,1
# 1,2,1
# ...
dataFile <- '../../data/amt-sense-mt-2013/munged/board-n.tsv';
data <- read.table(dataFile,header=T,comment.char='#');

# DATA
ii <- data[,1];    # item for labels
jj <- data[,2];    # annotator for labels
y <- data[,3];     # labels


I <- max(ii);      # number of items
J <- max(jj);      # number of annotators
K <- max(y);       # number of categories
N <- dim(data)[1]; # total number of observations

# HYPERPARAMETERS
alpha <- rep(2,K);              
beta <- matrix(2,K,K);          
for (k in 1:K) beta[k,k] = 20;
print("alpha=");
print(alpha);
print("beta=");
print(beta);

# INITS
piVote <- rep(0,K);
for (n in 1:N)
  piVote[y[n]] <- piVote[y[n]] + 1;
piVote <- piVote / sum(piVote)
print("voted pi=")
print(piVote);

votes <- matrix(0,I,K);
for (n in 1:N)
  votes[ii[n],y[n]] <- votes[ii[n],y[n]] + 1;

zVote <- rep(0,I);
for (i in 1:I)
  zVote[i] <- max_index(votes[i,]);

pluralityVotePercentage <- rep(NA,I);
for (i in 1:I)
    pluralityVotePercentage[i] <- votes[i,zVote[i]] / sum(votes[i,]); 
hist(pluralityVotePercentage,xlim=c(0,1),breaks=20);


thetaVote <- array(2,c(J,K,K)); 
for (j in 1:J)
  for (k in 1:K)
    thetaVote[j,k,k] <- 5;
for (n in 1:N) 
  thetaVote[jj[n],zVote[ii[n]],y[n]] <- thetaVote[jj[n],zVote[ii[n]],y[n]] + 1;
for (j in 1:J) 
  for (k in 1:K)
    thetaVote[j,k,] <- thetaVote[j,k,] / sum(thetaVote[j,k,]);

data_array <- c("I","J","K","N","ii","jj","y","alpha","beta");

print(data_array);

init_fun = function() { 
  return(list(pi=piVote,
              theta=thetaVote));
}

print(Sys.time());
print("fit");

fit <- stan(file="dawid-skene-gen-Z.stan",
           data=data_array,
           init=init_fun,
           iter=80, chains=5);

print(Sys.time());
print("extract")

fit_ss <- extract(fit);

print("average expected Z from samples");

S <- dim(fit_ss$expected_Z)[1];
avg_expected_Z <- matrix(0,nrow=I,ncol=K);
for (s in 1:S) 
    avg_expected_Z[] <- avg_expected_Z[] + fit_ss$expected_Z[s,,];
avg_expected_Z <- avg_expected_Z/S;

print("compare");

zModel <- rep(0,I);
diffs <- data.frame();
for (i in 1:I) {
    zModel[i] <- max_index(avg_expected_Z[i,]);
    if (zModel[i] != zVote[i]) {
       diffs <- rbind(diffs,data[data[,1]==i,]);
    }
}

agree <- zVote == zModel;
pct_agree <- 100*(summary(as.factor(agree))[2]/I);
cat("agreement between voted truth and modeled truth: ",pct_agree,"\n");

print(Sys.time());
#print("save");

#save(fit_ss,data,ii,I,jj,J,N,y,K,diffs,file="board-n-1_ds_Z_200samples.RData");
#print(Sys.time());


print("done");
