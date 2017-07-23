dpkg --add-architecture i386
apt-get update
apt-get install libc6:i386 libncurses5:i386 libstdc++6:i386 openjdk-8-jre openjdk-8-jdk unzip iftop vim tcpdump tcpflow iperf traceroute wget curl 
apt-get upgrade
systemctl enable rvrd.service || true