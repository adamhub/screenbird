#! /bin/bash
sudo apt-get remove ffmpeg x264 libx264-dev yasm
sudo apt-get update
sudo apt-get install -y build-essential git-core checkinstall texi2html libfaac-dev libopencore-amrnb-dev libopencore-amrwb-dev libtheora-dev libvorbis-dev zlib1g-dev python-pip mencoder
sudo pip install pika==0.9.5
sudo pip install boto==1.9b
sudo pip install requests==0.14.2

cd
wget http://www.tortall.net/projects/yasm/releases/yasm-1.2.0.tar.gz
tar xzvf yasm-1.2.0.tar.gz
cd yasm-1.2.0
./configure
make
sudo checkinstall --pkgname=yasm --pkgversion="1.2.0" --backup=no --deldoc=yes --default

cd
git clone --depth 1 git://git.videolan.org/x264
cd x264
./configure --enable-static
make
sudo checkinstall --pkgname=x264 --default --pkgversion="3:$(./version.sh |  awk -F'[" ]' '/POINT/{print $4"+git"$5}')" --backup=no --deldoc=yes

sudo apt-get remove libmp3lame-dev
sudo apt-get install nasm
cd
wget http://downloads.sourceforge.net/project/lame/lame/3.99/lame-3.99.5.tar.gz
tar xzvf lame-3.99.5.tar.gz
cd lame-3.99.5
./configure --enable-nasm --disable-shared
make
sudo checkinstall --pkgname=lame-ffmpeg --pkgversion="3.99.5" --backup=no --default --deldoc=yes

cd
git clone --depth 1 http://git.chromium.org/webm/libvpx.git
cd libvpx
./configure
make
sudo checkinstall --pkgname=libvpx --pkgversion="$(date +%Y%m%d%H%M)-git" --backup=no --default --deldoc=yes

cd
git clone --depth 1 git://source.ffmpeg.org/ffmpeg
cd ffmpeg
./configure --enable-gpl --enable-libfaac --enable-libmp3lame --enable-libopencore-amrnb --enable-libopencore-amrwb --enable-libtheora --enable-libvorbis --enable-libvpx --enable-libx264 --enable-nonfree --enable-version3
make
sudo checkinstall --pkgname=ffmpeg --pkgversion="5:$(./version.sh)" --backup=no --deldoc=yes --default
hash x264 ffmpeg ffplay ffprob
cd
python encoder.py

