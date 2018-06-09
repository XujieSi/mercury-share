package edu.gatech.application;

//import java.io.IOException;
//import Sobel.TextFile.Mode;

public class RgbImage {
	public int width;
	public int height;

	public int[][][] image;
	public RgbImage() {}

	
	public void set(int [][][] img){
		height = img.length;
		image = new int[height][][];
		for (int i=0; i<height; i++){
			width = img[i].length;
			image[i] = new int[width][];
			for(int j=0; j<width; j++){
				image[i][j] = new int[3];
				for(int k=0;k<3;++k) image[i][j][k] = img[i][j][k];
			}
		}
	}
	
	public void genRandomGraph(int W, int H){
		width = W;
		height = H;
		
		image = new int[height][][];
		for (int i=0; i<height; i++){
			image[i] = new int[width][];
			for(int j=0; j<width; j++){
				image[i][j] = new int[3];
			}
		}
		
		for (int i = 0; i < height; ++i) {
			for (int j = 0; j < width; j++) {
				image[i][j][0] = (i*j + i * 13 + j * 17) % 255;
				image[i][j][1] =  (i*j + i * 19 + j * 17) % 255;				
				image[i][j][2] = (i*i + j*j + i*23 + j * 17) % 255;				
			}
		}		
	}
	
	public void slideWindow(int x, int y, int[][][] window) {
		int h = window.length;
		int w = window[0].length;

		int x0 = x - (int)((float)w/2.);
		int x1 = x + (int)((float)w/2.);

		int y0 = y - (int)((float)h/2.);
		int y1 = y + (int)((float)h/2.);

		int r = 0;
		for (int j = y0; j <= y1; ++j) {
			if (j < 0 || j >= height) {
				for (int i = x0; i <= x1; ++i) {
					window[r][i-x0][0] = 0; 	
					window[r][i-x0][1] = 0; 	
					window[r][i-x0][2] = 0;	
				}
			} else {
				for (int i = x0; i <= x1; ++i) {
					if (i < 0 || i >= width) {
						window[r][i-x0][0] = 0; 	
						window[r][i-x0][1] = 0; 	
						window[r][i-x0][2] = 0;	
					} else {
						window[r][i-x0][0] = image[j][i][0]; 	
						window[r][i-x0][1] = image[j][i][1];	
						window[r][i-x0][2] = image[j][i][2];	
					}
				}
			}
			r++;
		}
	}

	public double sobel (int[][][] window) {
		double p1 = ( double)luminance(window[0][0]);	
		double p2 = ( double)luminance(window[0][1]);	
		double p3 = ( double)luminance(window[0][2]);	

		double p4 = ( double)luminance(window[1][0]);	
		double p5 = ( double)luminance(window[1][1]);	
		double p6 = ( double)luminance(window[1][2]);	

		double p7 = ( double)luminance(window[2][0]);	
		double p8 = ( double)luminance(window[2][1]);	
		double p9 = ( double)luminance(window[2][2]);	

		double x = (p1 + (p2 + p2) + p3 - p7 - (p8 + p8) - p9); 	
		double y = (p3 + (p6 + p6) + p9 - p1 - (p4 + p4) - p7);	

		double l = Math.sqrt(x * x + y * y);	

		return l;
	}

	public  int luminance( int[] rgb) {
		double rC = 0.30;	
		double gC = 0.59;	
		double bC = 0.11;	

		return ( int)(rC * rgb[0] + gC * rgb[1] + bC * rgb[2] + 0.5) % 256;	
	}

	public void makeGrayscale() {
		int i;
		int j;
		double luminance;

		double rC = 0.30 / 256.0;	
		double gC = 0.59 / 256.0;	
		double bC = 0.11 / 256.0;	

		for(i = 0; i < image.length; i++) {
			for(j = 0; j < image[i].length; j++) {
				luminance = rC * image[i][j][0] + gC * image[i][j][1] + bC * image[i][j][2];	

				image[i][j][0] = ( int)(luminance * 256);	
				image[i][j][1] = ( int)(luminance * 256);	
				image[i][j][2] = ( int)(luminance * 256);	
			}
		}
	}

	public static void work(int[][][] img ){
		RgbImage rgbImage = new RgbImage();
		rgbImage.set(img);
		//rgbImage.genRandomGraph(n, n);
		rgbImage.makeGrayscale();

		int[][][] window = new int[3][][];
		for(int i=0; i<3; i++){
			window[i] = new int[3][];
			for(int j=0; j<3; j++){
				window[i][j] = new int[3];
			}
		}
		double l;
		for (int y=0; y < rgbImage.height; y++) {
			for (int x=0; x < rgbImage.width; x++) {
				rgbImage.slideWindow(x,y, window);
				l = rgbImage.sobel(window);	
				int L = (int)(l);
				if (L >= 256)
					L = 255;
				if (L < 0)
					L = 0;
			}
		}
	}
	
	public static void work(int n){
		RgbImage rgbImage = new RgbImage();
		rgbImage.genRandomGraph(n, n);
		rgbImage.makeGrayscale();

		int[][][] window = new int[3][][];
		for(int i=0; i<3; i++){
			window[i] = new int[3][];
			for(int j=0; j<3; j++){
				window[i][j] = new int[3];
			}
		}
		double l;
		for (int y=0; y < rgbImage.height; y++) {
			for (int x=0; x < rgbImage.width; x++) {
				rgbImage.slideWindow(x,y, window);
				l = rgbImage.sobel(window);	
				int L = (int)(l);
				if (L >= 256)
					L = 255;
				if (L < 0)
					L = 0;
			}
		}
	}
	
}
