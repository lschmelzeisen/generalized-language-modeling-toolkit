dbUser="importer"
echo $1
inputPath=${1/\/typos*/}
dbLang=${inputPath##*/}
inputPath=${inputPath%/*}
dbType=${inputPath##*/}
echo $inputPath
echo $dbLang
echo $dbType

dbName=$dbType"_"$dbLang"_typo"
echo $dbName
#exit 1
#dbPath="/mnt/vdb/typoeval/mysql/${dbName}/" #server
dbPath=/var/lib/mysql/${dbName}/ #local machine

mysql -u ${dbUser} -e "drop database ${dbName};"
mysql -u ${dbUser} -e "create database ${dbName};"

path="$1*/*"
for file in $path
  do
  xpath=${file%/*}
  xbase=${file##*/}
  xfext=${xbase##*.}
  xpref=${xbase%.*}
  tablename=$xfext"_"$xpref;
  echo "tablename: "$tablename;
  echo "xpath: "$xpath;
  echo "xbase: "$xbase;
  echo "xfext: "$xfext;
  echo "xpref: "$xpref;

  #create tables and indices
  mysql -u ${dbUser} $dbName --local-infile=1 -e "create table ${tablename} (source varchar(60),target varchar(60),score float) engine=myisam character set utf8 collate utf8_bin;
  create index ${tablename}_ix on ${tablename} (source(60), score desc);
  create index ${tablename}_2_ix on ${tablename} (source(60), target(2), score desc);
  create index ${tablename}_3_ix on ${tablename} (source(60), target(3), score desc);
  create index ${tablename}_4_ix on ${tablename} (source(60), target(4), score desc);
  create index ${tablename}_5_ix on ${tablename} (source(60), target(5), score desc);"


  #disable indices
  myisamchk --keys-used=0 -rq ${dbPath}${tablename}

  mysql -u ${dbUser} $dbName --local-infile=1 -e "flush tables;"

  #import data
  mysql -u ${dbUser} $dbName --local-infile=1 -e "load data local infile '$file' into table ${tablename} fields terminated by '\t' enclosed by '' lines terminated by '\n' (source, target, score);"

  #compress table / really necessary?
  myisampack ${dbPath}${tablename}

  #enable index
  myisamchk -rq ${dbPath}${tablename} --tmpdir="/mnt/vdb/tmp" --sort_buffer=3G #--sort-index --sort-records=1

  #and flush index again.
  mysql -u ${dbUser} $dbName --local-infile=1 -e "flush tables;"

done
