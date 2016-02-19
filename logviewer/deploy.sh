export node=shuttle-0340

if scp target/EmbeddedTomcat-1.0-war-exec.jar root@${node}:/var/tmp/LogViewer.jar; then
  ssh root@${node} monit restart log-viewer
fi

