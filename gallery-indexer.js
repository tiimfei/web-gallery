var fs = require('fs');
// print process.argv
process.argv.forEach(function (val, index, array) {
  console.log(index + ': ' + val);
});
if (process.argv[2]!=null){
	  basedir =process.argv[2];
}else
	  basedir = __dirname;	

console.log("listing files under:" + basedir);
function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}
function endsWithin(str,suffix){
	 var result = false;
	 suffix.forEach(function(item){result = result ||endsWith(str,item);});
	 return result;
}

var traverseFileSystem = function (currentPath, folderjson) {
    console.log("traversing folder:" + currentPath);
    var files = fs.readdirSync(currentPath);
    var imagejson = fs.createWriteStream(currentPath+"/images.json", { flags: 'w',
  										encoding: 'utf8',
  										mode: 0666 });
     										
    var picCounter = 0;
    imagejson.write("{items:[");
    
    for (var i in files) {
       var currentFile = currentPath + '/' + files[i];
       console.log("currentFile:" + currentFile);
       
       var stats = fs.statSync(currentFile);
       if (stats.isFile()) {
      	 console.log(currentFile);
      	 //filtering jpg files
      	 
	       if(endsWithin(files[i],["jpg","JPG"])){
	       		 picCounter++;
	       		 imagejson.write("{");
	           imagejson.write("\"large\": \"" + currentFile + "\",\n");
	           imagejson.write("\"thumb\": \"" + currentFile + "\",\n");
	           imagejson.write("\"title\": \"" + currentFile + "\",\n");
	           imagejson.write("},");
	           
					}
					
       }
      else if (stats.isDirectory()) {
      			//filtering out utility and library folders
      			 if(!endsWithin(currentFile,["dojo","dijit","dojox","exclude"]))      			 	
             	picCounter +=traverseFileSystem(currentFile,folderjson);
           }
     }
    imagejson.write("]}");
    if(picCounter>0)
      	  	folderjson.write("{\"name\": \""+currentPath+"\"},\n");     	 
    return picCounter;
   };
   
var folderjson = fs.createWriteStream("folders.json", { flags: 'w',
  										encoding: 'utf8',
  										mode: 0666 });
folderjson.write("{items:[");
var  picCount = traverseFileSystem(basedir,folderjson);
folderjson.write("]}");
console.log("found "+ picCount+ " jpg files");