/*
 * JAVE - A Java Audio/Video Encoder (based on FFMPEG)
 * 
 * Copyright (C) 2008-2009 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.jave.locators;

/**
 * A package-private utility to add a shutdown hook to kill ongoing encoding
 * processes at the jvm shutdown.
 * 
 * @author Carlo Pelliccia
 */
class ProcessKiller extends Thread {

    /**
     * The process to kill.
     */
    private Process process;

    /**
     * Builds the killer.
     * 
     * @param process
     *            The process to kill.
     */
//    public ProcessKiller(Process process) {
//        this.process = process;
//    }

    /**
     * Creates thread to kill processes
     * @param process
     *             Process to kill
     * @param name 
     *             Name of process killer
     */
    ProcessKiller(Process process, String name) {
        super(name);
        this.process = process;
    }

    /**
     * It kills the supplied process.
     */
    @Override
    public void run() {
        process.destroy();
    }
}
