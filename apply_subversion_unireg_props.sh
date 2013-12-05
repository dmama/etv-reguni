#!/bin/bash
find . -not -regex ".*target.*" -name \*.xml -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.java -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.jsp -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.php -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.txt -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.css -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.js -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.sample -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.html -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.htm -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.htaccess -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.launch -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.tld -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.xsd -exec svn propset svn:eol-style native {} \;
find . -not -regex ".*target.*" -name \*.png -exec svn propset svn:mime-type image/png {} \;
find . -not -regex ".*target.*" -name \*.jpg -exec svn propset svn:mime-type image/jpeg {} \;
find . -not -regex ".*target.*" -name \*.gif -exec svn propset svn:mime-type image/gif {} \;
find . -not -regex ".*target.*" -name .project -exec svn propset svn:eol-style native {} \;
